package app.gamenative.cheats

object MemoryScannerJni {
    init { System.loadLibrary("memscanner") }
    external fun resolvePointerChain(pid: Int, baseAddr: Long, offsets: LongArray): Long
    external fun write(pid: Int, address: Long, value: Long, type: Int): Boolean
    external fun lock(pid: Int, address: Long, value: Long, type: Int)
    external fun unlock(pid: Int, address: Long)
    external fun unlockAll(pid: Int)
}
