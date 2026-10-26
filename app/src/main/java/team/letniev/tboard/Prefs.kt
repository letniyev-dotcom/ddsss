package team.letniev.tboard

import android.content.Context
import androidx.compose.ui.graphics.Color

/** theme: 0 авто, 1 светлая, 2 тёмная */
data class Cfg(
    val theme: Int = 0,
    val accent: Int = 0,
    val vibe: Boolean = true,
    val hints: Boolean = true,
    val autocap: Boolean = true,
    val lang: Int = 0,
)

val ACCENTS = listOf(
    Color(0xFF3E7BFA), Color(0xFF7C5CFC), Color(0xFFE0508A),
    Color(0xFFF08A2E), Color(0xFF22A67A), Color(0xFF18A5D6),
)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("tboard", Context.MODE_PRIVATE)

    fun load(c: Context): Cfg {
        val p = sp(c)
        return Cfg(
            theme = p.getInt("theme", 0),
            accent = p.getInt("accent", 0).coerceIn(0, ACCENTS.lastIndex),
            vibe = p.getBoolean("vibe", true),
            hints = p.getBoolean("hints", true),
            autocap = p.getBoolean("autocap", true),
            lang = p.getInt("lang", 0).coerceIn(0, LANGS.lastIndex),
        )
    }

    fun save(c: Context, f: Cfg) {
        sp(c).edit()
            .putInt("theme", f.theme)
            .putInt("accent", f.accent)
            .putBoolean("vibe", f.vibe)
            .putBoolean("hints", f.hints)
            .putBoolean("autocap", f.autocap)
            .putInt("lang", f.lang)
            .apply()
    }
}
