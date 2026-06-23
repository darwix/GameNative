package app.gamenative.cheats

enum class ValueType { INT32, INT64, FLOAT }

/**
 * A pre-defined memory cheat using a pointer chain from a Cheat Engine .CT file.
 *
 * Copy directly from the CT entry:
 *   <Address>"ModuleName.dll"+XXXXXXXX  →  moduleName + moduleOffset
 *   <Offsets><Offset>...</Offset>...    →  pointerOffsets (in order, hex)
 *
 * [lockValue] for FLOAT cheats: pass Float.toRawBits(value).toLong()
 *   e.g. stamina 270.0f → Float.toRawBits(270.0f).toLong() = 0x43870000L
 */
data class CheatDefinition(
    val id: String,
    val labelResId: Int,
    val valueType: ValueType,
    val lockValue: Long,
    val moduleName: String,          // e.g. "System.Private.CoreLib.dll"
    val moduleOffset: Long,          // e.g. 0x9DBD30L
    val pointerOffsets: List<Long>,  // e.g. [0xCE4L, 0x8L, ...]
)
