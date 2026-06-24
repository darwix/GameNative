package app.gamenative.cheats

import app.gamenative.utils.charToXKeycode
import com.winlator.xserver.XServer
import com.winlator.xserver.XKeycode
import timber.log.Timber

class CheatSession(
    private val pid: Int,
    private val xServer: XServer? = null,
) {
    private val activeInputCommands = mutableSetOf<String>()

    fun lock(cheat: CheatDefinition, value: String = "") {
        val action = cheat.action as CheatAction.InputCommand
        val keys = action.keys.map {
            if (it is String) it.replace("{value}", value) else it
        }
        typeKeys(keys)
        if (action.type is InputCommandType.Toggle) activeInputCommands += cheat.id
        Timber.tag(TAG).d("injected keys for ${cheat.id}")
    }

    fun unlock(cheatId: String) {
        if (activeInputCommands.remove(cheatId)) {
            Timber.tag(TAG).d("deactivated input command $cheatId")
        }
    }

    fun isLocked(cheatId: String): Boolean = activeInputCommands.contains(cheatId)

    fun cleanup() {
        Timber.tag(TAG).d("cleanup pid=$pid, inputCmds=${activeInputCommands.size}")
        activeInputCommands.clear()
    }

    private fun typeKeys(keys: List<Any>) {
        val server = xServer ?: run { Timber.tag(TAG).e("typeKeys: no XServer"); return }
        for (item in keys) {
            when (item) {
                is XKeycode -> {
                    server.injectKeyPress(item, 0)
                    Thread.sleep(50)
                    server.injectKeyRelease(item)
                    Thread.sleep(300)
                }
                is String -> {
                    for (ch in item) {
                        if (ch == '\t') { Thread.sleep(500); continue }
                        val keycode = charToXKeycode(ch)
                        Timber.tag(TAG).d("injectKeyPress $keycode keysym=0x${ch.code.toString(16)}")
                        server.injectKeyPress(keycode, ch.code)
                        Thread.sleep(30)
                        server.injectKeyRelease(keycode)
                        Thread.sleep(20)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CheatSession"
    }
}
