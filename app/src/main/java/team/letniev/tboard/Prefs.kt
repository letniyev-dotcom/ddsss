package team.letniev.tboard

import android.content.Context

/** цвета темы Telegram, пересчитанные под клавиатуру (ARGB) */
class TgTheme(
    val name: String,
    val bg: Int, val key: Int, val ent: Int, val tx: Int, val hint: Int, val ic: Int, val pop: Int,
    val acc: Int, val accBg: Int, val on: Int, val dark: Boolean,
) {
    fun encode(): String = listOf(
        name.replace(',', ' '), bg, key, ent, tx, hint, ic, pop, acc, accBg, on, if (dark) 1 else 0,
    ).joinToString(",")

    companion object {
        fun decode(s: String): TgTheme? {
            val p = s.split(',')
            if (p.size < 12) return null
            return try {
                TgTheme(
                    p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt(), p[6].toInt(), p[7].toInt(),
                    p[8].toInt(), p[9].toInt(), p[10].toInt(), p[11] == "1",
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/** theme: 0 авто, 1 светлая, 2 тёмная; accent — индекс в KACC */
data class Cfg(
    val theme: Int = 1,
    val accent: Int = 0,
    val vibe: Boolean = true,
    val hints: Boolean = true,
    val autocap: Boolean = true,
    val sugg: Boolean = true,
    val lang: Int = 0,
    val tgUse: Boolean = false,
    val tgOnly: Boolean = true,
    val tg: TgTheme? = null,
)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("tboard", Context.MODE_PRIVATE)

    fun load(c: Context): Cfg {
        val p = sp(c)
        return Cfg(
            theme = p.getInt("theme", 1),
            accent = p.getInt("accent", 0).coerceIn(0, KACC.lastIndex),
            vibe = p.getBoolean("vibe", true),
            hints = p.getBoolean("hints", true),
            autocap = p.getBoolean("autocap", true),
            sugg = p.getBoolean("sugg", true),
            lang = p.getInt("lang", 0).coerceIn(0, LANGS.lastIndex),
            tgUse = p.getBoolean("tgUse", false),
            tgOnly = p.getBoolean("tgOnly", true),
            tg = p.getString("tg", null)?.let { TgTheme.decode(it) },
        )
    }

    fun save(c: Context, f: Cfg) {
        sp(c).edit()
            .putInt("theme", f.theme)
            .putInt("accent", f.accent)
            .putBoolean("vibe", f.vibe)
            .putBoolean("hints", f.hints)
            .putBoolean("autocap", f.autocap)
            .putBoolean("sugg", f.sugg)
            .putInt("lang", f.lang)
            .putBoolean("tgUse", f.tgUse)
            .putBoolean("tgOnly", f.tgOnly)
            .putString("tg", f.tg?.encode())
            .apply()
    }
}
