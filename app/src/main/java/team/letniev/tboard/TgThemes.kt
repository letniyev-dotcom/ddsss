package team.letniev.tboard

import kotlin.math.abs
import kotlin.math.roundToInt

/** разбор файлов .attheme и подбор цветов клавиатуры (порт из HTML-прототипа) */
object TgThemes {
    private fun hex(h: String) = h.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    private fun color(v: String): IntArray? {
        val t = v.trim()
        if (t.startsWith("#")) {
            val h = t.substring(1)
            if (!hex(h)) return null
            return when (h.length) {
                8 -> intArrayOf(h.substring(2, 4).toInt(16), h.substring(4, 6).toInt(16), h.substring(6, 8).toInt(16))
                6 -> intArrayOf(h.substring(0, 2).toInt(16), h.substring(2, 4).toInt(16), h.substring(4, 6).toInt(16))
                else -> null
            }
        }
        val n = t.toLongOrNull() ?: return null
        val u = (n and 0xFFFFFFFFL).toInt()
        return intArrayOf((u shr 16) and 255, (u shr 8) and 255, u and 255)
    }

    fun parse(raw: ByteArray): Map<String, IntArray> {
        var txt = String(raw, Charsets.ISO_8859_1)
        val w = Regex("\\r?\\nWPS\\r?\\n").find(txt)
        if (w != null && w.range.first > 0) txt = txt.substring(0, w.range.first)
        val o = HashMap<String, IntArray>()
        txt.split(Regex("\\r?\\n")).forEach { l ->
            val m = Regex("^([A-Za-z0-9_]+)=(.+)$").find(l) ?: return@forEach
            color(m.groupValues[2])?.let { o[m.groupValues[1]] = it }
        }
        return o
    }

    private fun mix(a: IntArray, b: IntArray, t: Float) = IntArray(3) { (a[it] + (b[it] - a[it]) * t).roundToInt() }
    private fun lum(c: IntArray) = (.299f * c[0] + .587f * c[1] + .114f * c[2]) / 255f
    private fun dist(a: IntArray, b: IntArray) = (0..2).sumOf { abs(a[it] - b[it]) }
    private fun argb(c: IntArray) = (0xFF shl 24) or (c[0] shl 16) or (c[1] shl 8) or c[2]

    fun derive(o: Map<String, IntArray>, name: String): TgTheme? {
        fun g(vararg ks: String): IntArray? = ks.firstNotNullOfOrNull { o[it] }
        val w = intArrayOf(255, 255, 255)
        val acc = g("chat_messagePanelSend", "windowBackgroundWhiteBlueHeader", "telegram_color", "chats_actionBackground") ?: return null
        var bg = g("chat_emojiPanelBackground", "windowBackgroundGray") ?: intArrayOf(240, 240, 240)
        var key = g("chat_messagePanelBackground", "windowBackgroundWhite") ?: w
        val tx = g("chat_messagePanelText", "windowBackgroundWhiteBlackText") ?: intArrayOf(0, 0, 0)
        val dk = lum(bg) < .5f
        if (!dk) bg = mix(bg, acc, .07f)
        if (dist(key, bg) < 40) key = if (dk) mix(bg, w, .09f) else mix(bg, w, .7f)
        val hint = g("chat_messagePanelHint", "windowBackgroundWhiteGrayText") ?: intArrayOf(151, 151, 151)
        val ic = mix(g("chat_emojiBottomPanelIcon", "chat_messagePanelIcons") ?: hint, tx, .5f)
        return TgTheme(
            name = name,
            bg = argb(bg), key = argb(key), ent = argb(mix(bg, acc, if (dk) .35f else .22f)),
            tx = argb(tx), hint = argb(hint), ic = argb(ic),
            pop = argb(if (dk) mix(key, w, .12f) else key),
            acc = argb(acc), accBg = argb(if (dk) acc else mix(acc, w, .7f)),
            on = if (lum(acc) > .62f) 0xFF14141E.toInt() else 0xFFFFFFFF.toInt(),
            dark = dk,
        )
    }
}
