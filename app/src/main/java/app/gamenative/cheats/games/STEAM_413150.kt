package app.gamenative.cheats.games

import app.gamenative.cheats.CheatAction
import app.gamenative.cheats.CheatDefinition
import app.gamenative.cheats.InputCommandType
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
        CheatDefinition(    
            id = "upgrade_backpack",
            label = "Upgrade Backpack (Full)",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug backpack 36", XKeycode.KEY_ENTER)),
            section = "Inventory",
        ),
        CheatDefinition(
            id = "give_item",
            label = "Give Item (x999)",
            action = CheatAction.InputCommand(
                keys = listOf(XKeycode.KEY_T, "/debug item {value} 999", XKeycode.KEY_ENTER),
                type = InputCommandType.FreeText(hint = "Item ID (e.g. 388)"),
            ),
            section = "Inventory",
        ),

        CheatDefinition(
            id = "unlock_crafting_recipes",
            label = "Unlock All Crafting Recipes",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug crafting", XKeycode.KEY_ENTER)),
            section = "Inventory",
        ),
        CheatDefinition(
            id = "unlock_cooking_recipes",
            label = "Unlock All Cooking Recipes",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug cooking", XKeycode.KEY_ENTER)),
            section = "Inventory",
        ),

        // Farm
        CheatDefinition(
            id = "setup_big_farm",
            label = "Setup Big Farm",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug setupbigfarm", XKeycode.KEY_ENTER)),
            section = "Farm",
        ),

        CheatDefinition(
            id = "grow_animals",
            label = "Grow All Animals",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug growanimals", XKeycode.KEY_ENTER)),
            section = "Farm",
        ),
        CheatDefinition(
            id = "befriend_animals",
            label = "Max Animal Friendship",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug befriend 1000", XKeycode.KEY_ENTER)),
            section = "Farm",
        ),

        // Fishing
        CheatDefinition(
            id = "catch_all_fish",
            label = "Catch All Fish",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug catchallfish", XKeycode.KEY_ENTER)),
            section = "Fishing",
        ),

        // Relationships
        CheatDefinition(
            id = "max_friendships",
            label = "Max All Friendships",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug friendall", XKeycode.KEY_ENTER)),
            section = "Relationships",
        ),

        // Game
        CheatDefinition(
            id = "freeze_game_time",
            label = "Freeze Game Time",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/debug pausetime", XKeycode.KEY_ENTER)),
            section = "Game",
        ),

        // Physics
        CheatDefinition(
            id = "super_speed",
            label = "Super Speed",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/speed 5", XKeycode.KEY_ENTER)),
            section = "Physics",
        ),
        CheatDefinition(
            id = "warp_home",
            label = "Teleport Home",
            action = CheatAction.InputCommand(keys = listOf(XKeycode.KEY_T, "/warphome", XKeycode.KEY_ENTER)),
            section = "Physics",
        ),

    )
}
