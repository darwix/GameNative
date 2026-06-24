package app.gamenative.utils

import com.winlator.xserver.XKeycode

fun charToXKeycode(ch: Char): XKeycode = when (ch) {
    '\n'       -> XKeycode.KEY_ENTER
    ' '        -> XKeycode.KEY_SPACE
    '/'        -> XKeycode.KEY_SLASH
    '-'        -> XKeycode.KEY_MINUS
    '0'        -> XKeycode.KEY_0
    '1'        -> XKeycode.KEY_1
    '2'        -> XKeycode.KEY_2
    '3'        -> XKeycode.KEY_3
    '4'        -> XKeycode.KEY_4
    '5'        -> XKeycode.KEY_5
    '6'        -> XKeycode.KEY_6
    '7'        -> XKeycode.KEY_7
    '8'        -> XKeycode.KEY_8
    '9'        -> XKeycode.KEY_9
    'a', 'A'   -> XKeycode.KEY_A
    'b', 'B'   -> XKeycode.KEY_B
    'c', 'C'   -> XKeycode.KEY_C
    'd', 'D'   -> XKeycode.KEY_D
    'e', 'E'   -> XKeycode.KEY_E
    'f', 'F'   -> XKeycode.KEY_F
    'g', 'G'   -> XKeycode.KEY_G
    'h', 'H'   -> XKeycode.KEY_H
    'i', 'I'   -> XKeycode.KEY_I
    'j', 'J'   -> XKeycode.KEY_J
    'k', 'K'   -> XKeycode.KEY_K
    'l', 'L'   -> XKeycode.KEY_L
    'm', 'M'   -> XKeycode.KEY_M
    'n', 'N'   -> XKeycode.KEY_N
    'o', 'O'   -> XKeycode.KEY_O
    'p', 'P'   -> XKeycode.KEY_P
    'q', 'Q'   -> XKeycode.KEY_Q
    'r', 'R'   -> XKeycode.KEY_R
    's', 'S'   -> XKeycode.KEY_S
    't', 'T'   -> XKeycode.KEY_T
    'u', 'U'   -> XKeycode.KEY_U
    'v', 'V'   -> XKeycode.KEY_V
    'w', 'W'   -> XKeycode.KEY_W
    'x', 'X'   -> XKeycode.KEY_X
    'y', 'Y'   -> XKeycode.KEY_Y
    'z', 'Z'   -> XKeycode.KEY_Z
    else       -> XKeycode.KEY_SPACE
}
