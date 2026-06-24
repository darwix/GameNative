package app.gamenative.cheats

import app.gamenative.cheats.games.STEAM_Cheats_413150
import app.gamenative.data.GameSource

object CheatRegistry {
    private val all: List<KeyedCheatDefinition> = listOf(
        STEAM_Cheats_413150,
        // add more game files here
    )

    private val byKey = all.associateBy { it.gameSource to it.gameId }

    fun getCheats(source: GameSource, gameId: String): List<CheatDefinition> =
        byKey[source to gameId]?.cheats ?: emptyList()

    fun hasCheats(source: GameSource, gameId: String): Boolean =
        byKey.containsKey(source to gameId)

    fun getKeyedCheats(source: GameSource, gameId: String): KeyedCheatDefinition? =
        byKey[source to gameId]
}
