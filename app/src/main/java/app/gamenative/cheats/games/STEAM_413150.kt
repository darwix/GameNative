package app.gamenative.cheats.games

import app.gamenative.cheats.CheatAction
import app.gamenative.cheats.CheatDefinition
import app.gamenative.cheats.KeyedCheatDefinition
import app.gamenative.data.GameSource
import com.winlator.xserver.XKeycode
import java.io.File
import timber.log.Timber

// Stardew Valley (Steam appId 413150) — verified 1.6.15-24356
// Uses InputCommand: vanilla 1.6 supports chat commands when allowChatCheats=true.
// "\n" opens chat (game auto-prefixes "/"), "\t" = 200ms delay, last "\n" submits.
val STEAM_Cheats_413150: KeyedCheatDefinition = object : KeyedCheatDefinition {
    override val gameSource = GameSource.STEAM
    override val gameId = "413150"
    override val gameVersion = "1.6.15-24356"
    override fun prepareSave(containerRootDir: File) {
        val savesDir = File(containerRootDir, ".wine/drive_c/users/xuser/AppData/Roaming/StardewValley/Saves")
        if (!savesDir.exists()) {
            Timber.tag("STEAM_413150").w("prepareSave: saves dir not found at $savesDir")
            return
        }
        savesDir.walkTopDown()
            .filter { it.isFile && it.extension.isEmpty() }
            .forEach { saveFile ->
                try {
                    val content = saveFile.readText()
                    if (content.contains("<allowChatCheats>false</allowChatCheats>")) {
                        saveFile.writeText(content.replace(
                            "<allowChatCheats>false</allowChatCheats>",
                            "<allowChatCheats>true</allowChatCheats>"
                        ))
                        Timber.tag("STEAM_413150").d("patched allowChatCheats in ${saveFile.name}")
                    }
                } catch (e: Exception) {
                    Timber.tag("STEAM_413150").e(e, "failed to patch ${saveFile.name}")
                }
            }
    }

    override val cheats = listOf(
        // Player
        CheatDefinition(
            id = "unlimited_health",
            label = "Unlimited Health",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/heal", XKeycode.KEY_ENTER)),
            section = "Player",
        ),
        CheatDefinition(
            id = "unlimited_energy",
            label = "Unlimited Energy",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/energize", XKeycode.KEY_ENTER)),
            section = "Player",
        ),
        CheatDefinition(
            id = "infinite_money",
            label = "Infinite Money",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/money 9999999", XKeycode.KEY_ENTER)),
            section = "Player",
        ),

        // Inventory
        CheatDefinition(
            id = "unlimited_items",
            label = "Unlimited Items",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug iq ALL_ITEMS", XKeycode.KEY_ENTER)),
            section = "Inventory",
        ),

        // Game
        CheatDefinition(
            id = "freeze_game_time",
            label = "Freeze Game Time",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/freeze", XKeycode.KEY_ENTER)),
            section = "Game",
        ),

        // Physics
        CheatDefinition(
            id = "super_speed",
            label = "Super Speed",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/speed 5", XKeycode.KEY_ENTER)),
            section = "Physics",
        ),
    )
}
