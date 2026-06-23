#include <jni.h>
#include <android/log.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <errno.h>
#include <stdio.h>

#define LOG_TAG "MemScanner"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define VALUE_TYPE_INT32  0
#define VALUE_TYPE_INT64  1
#define VALUE_TYPE_FLOAT  2
#define MAX_LOCKS         64

typedef struct {
    int      active;
    pid_t    pid;
    uint64_t address;
    uint64_t value;
    int      type;
} LockEntry;

static LockEntry        g_locks[MAX_LOCKS];
static pthread_t        g_lock_thread;
static volatile int     g_thread_running = 0;
static pthread_mutex_t  g_mutex = PTHREAD_MUTEX_INITIALIZER;

/* ── size helpers ──────────────────────────────────── */

static size_t value_size(int type) {
    return (type == VALUE_TYPE_INT64) ? 8 : 4;
}

/* ── ptrace helpers ────────────────────────────────── */

static int attach_stop(pid_t pid) {
    if (ptrace(PTRACE_ATTACH, pid, NULL, NULL) < 0) {
        LOGE("ptrace attach pid=%d: %s", (int)pid, strerror(errno));
        return -1;
    }
    waitpid(pid, NULL, 0);
    return 0;
}

static void detach_cont(pid_t pid) {
    ptrace(PTRACE_DETACH, pid, NULL, NULL);
}

static int mem_pread(pid_t pid, uint64_t addr, void *buf, size_t len) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/mem", (int)pid);
    int fd = open(path, O_RDONLY);
    if (fd < 0) { LOGE("open /proc/%d/mem (r): %s", pid, strerror(errno)); return -1; }
    ssize_t n = pread(fd, buf, len, (off_t)addr);
    close(fd);
    return (n == (ssize_t)len) ? 0 : -1;
}

static int mem_pwrite(pid_t pid, uint64_t addr, void *data, size_t len) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/mem", (int)pid);
    int fd = open(path, O_WRONLY);
    if (fd < 0) { LOGE("open /proc/%d/mem (w): %s", pid, strerror(errno)); return -1; }
    ssize_t n = pwrite(fd, data, len, (off_t)addr);
    close(fd);
    return (n == (ssize_t)len) ? 0 : -1;
}

/* ── lock thread ───────────────────────────────────── */

static void *lock_thread_fn(void *arg) {
    (void)arg;
    while (g_thread_running) {
        /* Snapshot active entries under the mutex */
        LockEntry snapshot[MAX_LOCKS];
        int count = 0;
        pthread_mutex_lock(&g_mutex);
        for (int i = 0; i < MAX_LOCKS; i++) {
            if (g_locks[i].active) snapshot[count++] = g_locks[i];
        }
        pthread_mutex_unlock(&g_mutex);

        /* Do ptrace work outside the mutex */
        for (int i = 0; i < count; i++) {
            LockEntry *e = &snapshot[i];
            if (attach_stop(e->pid) == 0) {
                mem_pwrite(e->pid, e->address, &e->value, value_size(e->type));
                detach_cont(e->pid);
            }
        }
        usleep(100 * 1000);
    }
    return NULL;
}

static void ensure_thread_running(void) {
    if (!g_thread_running) {
        g_thread_running = 1;
        pthread_create(&g_lock_thread, NULL, lock_thread_fn, NULL);
    }
}

/* ── JNI: resolvePointerChain ──────────────────────── */
/*
 * Resolves a CE-style pointer chain under ptrace.
 * offsets[0..count-2]: each is added then dereferenced (read 8-byte pointer)
 * offsets[count-1]:    added but NOT dereferenced — this is the final field address
 * Returns resolved address, or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_app_gamenative_cheats_MemoryScannerJni_resolvePointerChain(
        JNIEnv *env, jobject thiz,
        jint pid, jlong base_addr, jlongArray j_offsets) {
    (void)env; (void)thiz;

    jsize count = (*env)->GetArrayLength(env, j_offsets);
    if (count == 0) return base_addr;

    jlong *offsets = (*env)->GetLongArrayElements(env, j_offsets, NULL);
    if (!offsets) return 0;

    uint64_t addr = (uint64_t)base_addr;
    uint64_t result = 0;

    if (attach_stop((pid_t)pid) < 0) {
        (*env)->ReleaseLongArrayElements(env, j_offsets, offsets, JNI_ABORT);
        return 0;
    }

    /* Read the initial pointer at base_addr */
    uint64_t ptr = 0;
    if (mem_pread((pid_t)pid, addr, &ptr, 8) < 0) {
        LOGE("resolvePointerChain: failed to read base ptr @ 0x%llx", (unsigned long long)addr);
        detach_cont((pid_t)pid);
        (*env)->ReleaseLongArrayElements(env, j_offsets, offsets, JNI_ABORT);
        return 0;
    }

    for (jsize i = 0; i < count; i++) {
        ptr += (uint64_t)offsets[i];
        if (i < count - 1) {
            /* Dereference: read next pointer */
            uint64_t next = 0;
            if (mem_pread((pid_t)pid, ptr, &next, 8) < 0) {
                LOGE("resolvePointerChain: failed at step %d addr=0x%llx", (int)i, (unsigned long long)ptr);
                ptr = 0;
                break;
            }
            ptr = next;
        }
        /* Last iteration: ptr is already the final address (last offset added, not dereferenced) */
    }

    result = ptr;
    detach_cont((pid_t)pid);
    (*env)->ReleaseLongArrayElements(env, j_offsets, offsets, JNI_ABORT);

    LOGD("resolvePointerChain: resolved to 0x%llx", (unsigned long long)result);
    return (jlong)result;
}

/* ── JNI: write ────────────────────────────────────── */

JNIEXPORT jboolean JNICALL
Java_app_gamenative_cheats_MemoryScannerJni_write(
        JNIEnv *env, jobject thiz, jint pid, jlong address, jlong value, jint type) {
    (void)env; (void)thiz;
    if (attach_stop((pid_t)pid) < 0) return JNI_FALSE;
    int r = mem_pwrite((pid_t)pid, (uint64_t)address, &value, value_size(type));
    detach_cont((pid_t)pid);
    return r == 0 ? JNI_TRUE : JNI_FALSE;
}

/* ── JNI: lock ─────────────────────────────────────── */

JNIEXPORT void JNICALL
Java_app_gamenative_cheats_MemoryScannerJni_lock(
        JNIEnv *env, jobject thiz, jint pid, jlong address, jlong value, jint type) {
    (void)env; (void)thiz;
    pthread_mutex_lock(&g_mutex);
    int slot = -1;
    for (int i = 0; i < MAX_LOCKS; i++) {
        if (g_locks[i].active &&
            g_locks[i].pid == (pid_t)pid &&
            g_locks[i].address == (uint64_t)address) {
            slot = i; break;
        }
        if (slot < 0 && !g_locks[i].active) slot = i;
    }
    if (slot < 0) {
        LOGE("lock table full");
    } else {
        g_locks[slot] = (LockEntry){
            .active = 1, .pid = (pid_t)pid,
            .address = (uint64_t)address, .value = (uint64_t)value, .type = type
        };
        ensure_thread_running();
    }
    pthread_mutex_unlock(&g_mutex);
}

/* ── JNI: unlock ───────────────────────────────────── */

JNIEXPORT void JNICALL
Java_app_gamenative_cheats_MemoryScannerJni_unlock(
        JNIEnv *env, jobject thiz, jint pid, jlong address) {
    (void)env; (void)thiz;
    pthread_mutex_lock(&g_mutex);
    for (int i = 0; i < MAX_LOCKS; i++) {
        if (g_locks[i].active &&
            g_locks[i].pid == (pid_t)pid &&
            g_locks[i].address == (uint64_t)address) {
            g_locks[i].active = 0; break;
        }
    }
    pthread_mutex_unlock(&g_mutex);
}

/* ── JNI: unlockAll ────────────────────────────────── */

JNIEXPORT void JNICALL
Java_app_gamenative_cheats_MemoryScannerJni_unlockAll(
        JNIEnv *env, jobject thiz, jint pid) {
    (void)env; (void)thiz;
    pthread_mutex_lock(&g_mutex);
    for (int i = 0; i < MAX_LOCKS; i++) {
        if (g_locks[i].pid == (pid_t)pid) g_locks[i].active = 0;
    }
    pthread_mutex_unlock(&g_mutex);
}
