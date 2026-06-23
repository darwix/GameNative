package app.gamenative.cheats.games

import app.gamenative.R
import app.gamenative.cheats.CheatDefinition
import app.gamenative.cheats.KeyedCheatDefinition
import app.gamenative.cheats.ValueType
import app.gamenative.data.GameSource

// Stardew Valley (Steam appId 413150)
// Pointer chains from Stardew Valley.CT (verified format, game version unknown — re-export .CT if
// chains stop resolving after a major game update).
//
// CASH chain:   System.Private.Xml.dll+7E3BE0 → [87C,450,0,AE4,10,168,0]   (INT32)
// STAMINA chain: System.Private.CoreLib.dll+9DBD30 → [CE4,8,3F8,18,728,90,90] (Float=270.0f)
val STEAM_Cheats_413150: KeyedCheatDefinition = object : KeyedCheatDefinition {
    override val gameSource = GameSource.STEAM
    override val gameId = "413150"
    override val cheats = listOf(
        CheatDefinition(
            id = "infinite_money",
            labelResId = R.string.cheat_infinite_money,
            valueType = ValueType.INT32,
            lockValue = 999999L,
            moduleName = "System.Private.Xml.dll",
            moduleOffset = 0x7E3BE0L,
            pointerOffsets = listOf(0x87CL, 0x450L, 0x0L, 0xAE4L, 0x10L, 0x168L, 0x0L),
        ),
        CheatDefinition(
            id = "infinite_stamina",
            labelResId = R.string.cheat_infinite_stamina,
            valueType = ValueType.FLOAT,
            lockValue = java.lang.Float.floatToRawIntBits(270.0f).toLong(),  // 0x43870000
            moduleName = "System.Private.CoreLib.dll",
            moduleOffset = 0x9DBD30L,
            pointerOffsets = listOf(0xCE4L, 0x8L, 0x3F8L, 0x18L, 0x728L, 0x90L, 0x90L),
        ),
    )
}
