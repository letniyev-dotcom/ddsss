package team.letniev.tboard

class Lang(
    val name: String, val abc: String, val rows: List<String>, val hints: List<String>,
    val start: List<String>, val dict: List<String>,
)

val LANGS = listOf(
    Lang(
        "Русский", "АБВ",
        listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбю"),
        listOf("1234567890", "@#₽%&-+()/~", "*\"':;!ъ?_"),
        listOf("привет", "я", "ты"),
        listOf(
            "привет", "приветик", "пока", "как", "дела", "спасибо", "пожалуйста", "хорошо", "ладно", "давай",
            "да", "нет", "ну", "ты", "я", "мы", "вы", "сегодня", "завтра", "когда", "где", "что", "почему",
            "можно", "нужно", "буду", "будет", "есть", "это", "так", "тоже", "потом", "сейчас", "ссылка",
            "шпаргалка", "встреча", "созвон", "дизайн",
        ),
    ),
    Lang(
        "English", "ABC",
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
        listOf("1234567890", "@#\$%&-+()", "*\"':;!?"),
        listOf("hi", "the", "and"),
        listOf(
            "hi", "hello", "thanks", "please", "good", "okay", "yes", "no", "the", "and", "you", "what",
            "when", "where", "today", "tomorrow", "link", "meeting", "design",
        ),
    ),
)

/** две страницы символов (как в HTML) */
val SYMBOLS = listOf(
    listOf("1234567890", "&#₽%_-+()/", "@*\"':;!?"),
    listOf("`~|•√π÷×¶∆", "£€¥¢^°{}\\©", "[]<>≈≠«»"),
)

/** символы на долгое нажатие для страниц символов */
val SYMHINTS = listOf(
    listOf("", "§№\$‰-—±<>\\", "~…“‘{}¡¿"),
    listOf("", "", ""),
)
