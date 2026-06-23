package app.gamenative.cheats

import timber.log.Timber
import java.io.File

class CheatSession(private val pid: Int) {
    private val lockedAddresses = mutableMapOf<String, Long>()

    fun lock(cheat: CheatDefinition) {
        val moduleBase = resolveModuleBase(pid, cheat.moduleName)
        if (moduleBase == 0L) {
            Timber.e("CheatSession: module '${cheat.moduleName}' not found in /proc/$pid/maps")
            return
        }
        val baseAddr = moduleBase + cheat.moduleOffset
        val resolved = MemoryScannerJni.resolvePointerChain(
            pid, baseAddr, cheat.pointerOffsets.toLongArray()
        )
        if (resolved == 0L) {
            Timber.e("CheatSession: pointer chain failed for ${cheat.id}")
            return
        }
        lockedAddresses[cheat.id] = resolved
        MemoryScannerJni.lock(pid, resolved, cheat.lockValue, cheat.valueType.ordinal)
        Timber.d("CheatSession: locked ${cheat.id} → 0x${resolved.toString(16)}")
    }

    fun unlock(cheatId: String) {
        val address = lockedAddresses.remove(cheatId) ?: return
        MemoryScannerJni.unlock(pid, address)
        Timber.d("CheatSession: unlocked $cheatId")
    }

    fun isLocked(cheatId: String): Boolean = lockedAddresses.containsKey(cheatId)

    fun cleanup() {
        Timber.d("CheatSession: cleanup pid=$pid, active=${lockedAddresses.size}")
        MemoryScannerJni.unlockAll(pid)
        lockedAddresses.clear()
    }

    private fun resolveModuleBase(pid: Int, moduleName: String): Long {
        return try {
            File("/proc/$pid/maps").useLines { lines ->
                lines.firstOrNull { line ->
                    line.contains(moduleName, ignoreCase = true) && line.contains("r-xp")
                }?.substringBefore('-')?.trimStart()?.toLongOrNull(16) ?: 0L
            }
        } catch (e: Exception) {
            Timber.e(e, "CheatSession: failed to read /proc/$pid/maps")
            0L
        }
    }
}
