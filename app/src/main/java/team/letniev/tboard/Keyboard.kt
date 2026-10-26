package team.letniev.tboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface KbActions {
    fun text(s: String)
    fun backspace()
    fun enter()
    fun switchLang()
}

private class Pal(
    val bg: Color, val key: Color, val special: Color,
    val press: Color, val text: Color, val hint: Color,
)

private val DARK = Pal(
    Color(0xFF0F1118), Color(0xFF242836), Color(0xFF1A1D28),
    Color(0xFF3A4054), Color(0xFFE8EAF2), Color(0xFF8B91A8),
)
private val LIGHT = Pal(
    Color(0xFFE6E8EF), Color(0xFFFFFFFF), Color(0xFFCDD2DE),
    Color(0xFFD4D9E6), Color(0xFF1A1C24), Color(0xFF7A8096),
)

private val LocalVibe = compositionLocalOf { true }

@Composable
private fun RowScope.Key(
    label: String,
    w: Float,
    bg: Color,
    fg: Color,
    press: Color,
    hint: String? = null,
    hintColor: Color = Color.Gray,
    fly: Boolean = false,
    repeat: Boolean = false,
    fontSize: TextUnit = 22.sp,
    onLong: (() -> Unit)? = null,
    onTap: () -> Unit,
) {
    var down by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLong)
    val prog = remember { Animatable(0f) }
    val view = LocalView.current
    val vibe = LocalVibe.current

    Box(
        Modifier
            .weight(w)
            .fillMaxHeight()
            .background(if (down) press else bg, RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    down = true
                    if (vibe) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    var fired = false
                    val job = scope.launch {
                        delay(380)
                        fired = true
                        if (repeat) {
                            while (true) {
                                tap()
                                delay(55)
                            }
                        } else {
                            long?.invoke()
                        }
                    }
                    val ok = tryAwaitRelease()
                    job.cancel()
                    down = false
                    if (ok && !fired) {
                        tap()
                        if (fly) scope.launch {
                            prog.snapTo(0f)
                            prog.animateTo(1f, tween(450))
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = fontSize, maxLines = 1)
        if (hint != null) {
            Text(
                hint, color = hintColor, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
            )
        }
        if (fly) {
            // мини-буква вылетает из клавиши и растворяется
            Text(
                label, color = fg, fontSize = fontSize,
                modifier = Modifier.graphicsLayer {
                    val p = prog.value
                    translationY = -26.dp.toPx() * p
                    scaleX = 1f - 0.4f * p
                    scaleY = 1f - 0.4f * p
                    alpha = if (p <= 0.001f || p >= 0.999f) 0f else 0.9f * (1f - p)
                },
            )
        }
    }
}

@Composable
private fun KRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(vertical = 3.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        content = content,
    )
}

/** shift: 0 выкл, 1 на одну букву, 2 caps lock */
@Composable
fun KeyboardRoot(cfg: Cfg, shift: Int, onShift: (Int) -> Unit, act: KbActions) {
    val dark = when (cfg.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val pal = if (dark) DARK else LIGHT
    val acc = ACCENTS[cfg.accent.coerceIn(0, ACCENTS.lastIndex)]
    val lang = LANGS[cfg.lang.coerceIn(0, LANGS.lastIndex)]
    var mode by remember { mutableIntStateOf(0) } // 0 буквы, 1 и 2 страницы символов

    CompositionLocalProvider(LocalVibe provides cfg.vibe) {
        Column(
            Modifier.fillMaxWidth().background(pal.bg).padding(horizontal = 4.dp).padding(bottom = 8.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    lang.name + " · TBoard",
                    color = pal.hint, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp),
                )
            }

            if (mode == 0) {
                lang.rows.forEachIndexed { ri, row ->
                    val side = (11 - row.length) / 2f
                    KRow {
                        if (ri < 2) {
                            if (side > 0f) Spacer(Modifier.weight(side))
                        } else {
                            Key(
                                if (shift == 2) "⇪" else "⇧", side,
                                if (shift > 0) acc else pal.special,
                                if (shift > 0) Color.White else pal.text, pal.press,
                                onLong = { onShift(2) },
                            ) { onShift(if (shift == 0) 1 else 0) }
                        }
                        row.forEachIndexed { i, c ->
                            val ch = if (shift > 0) c.uppercase() else c.toString()
                            val h = if (cfg.hints) lang.hints[ri].getOrNull(i)?.toString() else null
                            Key(
                                ch, 1f, pal.key, pal.text, pal.press,
                                hint = h, hintColor = pal.hint, fly = true,
                                onLong = {
                                    val hh = lang.hints[ri].getOrNull(i)?.toString()
                                    if (hh != null) act.text(hh)
                                },
                            ) { act.text(ch) }
                        }
                        if (ri < 2) {
                            if (side > 0f) Spacer(Modifier.weight(side))
                        } else {
                            Key("⌫", side, pal.special, pal.text, pal.press, repeat = true) { act.backspace() }
                        }
                    }
                }
            } else {
                SYMBOLS[mode - 1].forEachIndexed { ri, row ->
                    val side = (11 - row.length) / 2f
                    KRow {
                        if (ri < 2) {
                            Spacer(Modifier.weight(side))
                        } else {
                            Key(
                                if (mode == 1) "1/2" else "2/2", side, pal.special, pal.text, pal.press,
                                fontSize = 15.sp,
                            ) { mode = if (mode == 1) 2 else 1 }
                        }
                        row.forEach { c ->
                            Key(c.toString(), 1f, pal.key, pal.text, pal.press, fly = true) { act.text(c.toString()) }
                        }
                        if (ri < 2) {
                            Spacer(Modifier.weight(side))
                        } else {
                            Key("⌫", side, pal.special, pal.text, pal.press, repeat = true) { act.backspace() }
                        }
                    }
                }
            }

            KRow {
                Key(
                    if (mode == 0) "?123" else lang.abc, 1.6f, pal.special, pal.text, pal.press,
                    fontSize = 15.sp,
                ) { mode = if (mode == 0) 1 else 0 }
                Key(",", 1f, pal.key, pal.text, pal.press) { act.text(",") }
                Key(
                    lang.name, 5f, pal.key, pal.hint, pal.press, fontSize = 13.sp,
                    onLong = { act.switchLang() },
                ) { act.text(" ") }
                Key(".", 1f, pal.key, pal.text, pal.press) { act.text(".") }
                Key("↵", 1.6f, acc, Color.White, pal.press) { act.enter() }
            }
        }
    }
}
