package app.gamenative.cheats

import app.gamenative.data.GameSource

interface KeyedCheatDefinition {
    val gameSource: GameSource
    val gameId: String
    val cheats: List<CheatDefinition>
}
