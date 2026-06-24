package app.gamenative.cheats

sealed class InputCommandType {
    object Toggle : InputCommandType()
    data class FreeText(val hint: String = "") : InputCommandType()
    data class Dropdown(val options: List<Pair<String, String>>) : InputCommandType()
}

sealed class CheatAction {
    // keys: XKeycode (raw press keysym=0, 300ms after) or String (char by char, \t=delay, {value}=substitution)
    data class InputCommand(
        val keys: List<Any>,
        val type: InputCommandType = InputCommandType.Toggle,
        val onDisable: List<Any> = emptyList(),
    ) : CheatAction()
}

sealed class CheatEvent {
    data class Toggle(val enabled: Boolean) : CheatEvent()
    data class Execute(val value: String = "") : CheatEvent()
}

data class CheatDefinition(
    val id: String,
    val label: String,
    val action: CheatAction,
    val section: String = "",
)
