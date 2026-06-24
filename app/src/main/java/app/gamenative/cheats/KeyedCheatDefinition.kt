package app.gamenative.cheats

import app.gamenative.data.GameSource
import java.io.File

interface KeyedCheatDefinition {
    val gameSource: GameSource
    val gameId: String
    val gameVersion: String  // e.g. "1.6.14" — pointer chains are version-specific
    val cheats: List<CheatDefinition>

    // Called once before first cheat toggle. Override to patch save files or config.
    // containerRootDir is the Wine container root (contains .wine/drive_c/...).
    fun prepareSave(containerRootDir: File) {}
}
