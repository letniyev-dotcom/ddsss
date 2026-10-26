package team.letniev.tboard

class Lang(val name: String, val abc: String, val rows: List<String>, val hints: List<String>)

val LANGS = listOf(
    Lang(
        "Русский", "АБВ",
        listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбю"),
        listOf("1234567890", "@#₽%&-+()/~", "*\"':;!ъ?_"),
    ),
    Lang(
        "English", "ABC",
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
        listOf("1234567890", "@#\$%&-+()", "*\"':;!?"),
    ),
)

/** две страницы символов */
val SYMBOLS = listOf(
    listOf("1234567890", "@#₽_&-+()/", "*\"':;!?"),
    listOf("~`|•√π÷×¶∆", "£\$€¢^°={}\\", "%©®™✓[]<>"),
)
