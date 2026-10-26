package team.letniev.tboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import kotlin.math.abs
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface KbActions {
    fun text(s: String)
    fun backspace()
    fun enter()
    fun switchLang(dir: Int = 1)
    fun hide()
    fun pickIme()
    fun replaceWord(old: String, new: String)
}

/* ---------- палитра из HTML (KP + ACC) ---------- */

private class Pal(
    val dark: Boolean, val bg: Color, val key: Color, val fn: Color, val ent: Color,
    val tx: Color, val hint: Color, val ic: Color, val pop: Color,
)

private val LIGHT = Pal(
    false, Color(0xFFE7EAF2), Color(0xFFFFFFFF), Color(0xFFD3D8E6), Color(0xFFC9D1E3),
    Color(0xFF1B1B1F), Color(0xFF7B8090), Color(0xFF44474E), Color(0xFFFFFFFF),
)
private val DARK = Pal(
    true, Color(0xFF181A26), Color(0xFF262A33), Color(0xFF353944), Color(0xFF414753),
    Color(0xFFFFFFFF), Color(0xFFA3A7B6), Color(0xFFE3E4EA), Color(0xFF4A4F5F),
)

/** l — светлая, d — тёмная, lo/dk — цвет «на акценте» */
private class KAcc(val l: Color, val d: Color, val lo: Color, val dk: Color)

private val KACC = listOf(
    KAcc(Color(0xFF2F64C9), Color(0xFFA8C7FA), Color.White, Color(0xFF062E6F)), // синий
    KAcc(Color(0xFF00796B), Color(0xFF80CBC4), Color.White, Color(0xFF00332D)), // бирюзовый
    KAcc(Color(0xFF2E7D32), Color(0xFFA5D6A7), Color.White, Color(0xFF0A3A10)), // зелёный
    KAcc(Color(0xFFC75B12), Color(0xFFFFB784), Color.White, Color(0xFF4A2000)), // оранжевый
    KAcc(Color(0xFFB5367A), Color(0xFFFFAFD3), Color.White, Color(0xFF5A0A35)), // розовый
    KAcc(Color(0xFF6B4FC9), Color(0xFFCFBCFF), Color.White, Color(0xFF2E1A73)), // фиолетовый
)

/** порядок акцентов в приложении (синий, фиолет, розовый, оранж, зелёный, голубой) → KACC */
private val ACC_MAP = intArrayOf(0, 5, 4, 3, 2, 1)

private val LocalVibe = compositionLocalOf { true }
private val LocalDim = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/* ---------- иконки из HTML (svg-пути 1 в 1) ---------- */

private class P(val d: String, val sw: Float? = null)
private class Ic(val p: List<P>, val vb: List<Float> = listOf(0f, 0f, 24f, 24f), val fillOnly: Boolean = false)

private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r $r" +
        "v${h - 2 * r}a$r $r 0 0 1 -$r $r" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 -$r -$r" +
        "v${-(h - 2 * r)}a$r $r 0 0 1 $r -${r}z"

private const val SHIFT_D = "M12 4 4.5 12H9v7h6v-7h4.5Z"
private val IC_SHIFT = Ic(listOf(P(SHIFT_D)))
private val IC_LOCK = Ic(listOf(P(SHIFT_D), P("M8.5 21.5h7")))
private val IC_BACK = Ic(listOf(P("M9.5 5H19a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H9.5L3 12Z"), P("m12.5 9.5 5 5m0-5-5 5")))
private val IC_ENTER = Ic(listOf(P("M20 5v6a3 3 0 0 1-3 3H5m0 0 4.5-4.5M5 14l4.5 4.5")))
private val IC_SPACE = Ic(listOf(P("M3.5 10v5.5h17V10")))
private val IC_CHEV = Ic(listOf(P("m5.5 9.5 6.5 6.5 6.5-6.5", 1.9f)))
private val IC_KBD = Ic(
    listOf(
        P("M4 7h.01M8 7h.01M12 7h.01M16 7h.01M20 7h.01M4 11h.01M8 11h.01M12 11h.01M16 11h.01M20 11h.01", 2.4f),
        P("M7 16h10"),
    ),
)
private val IC_MIC = Ic(listOf(P(rr(9f, 3f, 6f, 11f, 3f)), P("M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3")))
private val IC_CLIP = Ic(
    listOf(
        P(rr(5f, 4f, 14f, 17f, 3f)),
        P("M9 4V3.5A1.5 1.5 0 0 1 10.5 2h3A1.5 1.5 0 0 1 15 3.5V4M9 11h6M9 15h4"),
    ),
)
private val IC_TG = Ic(
    listOf(
        P(
            "M16.906 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z",
        ),
    ),
    vb = listOf(3.2f, 5.2f, 15f, 13f), fillOnly = true,
)

@Composable
private fun Ico(ic: Ic, size: Dp, color: Color, fill: Boolean = false) {
    val paths = remember(ic) { ic.p.map { PathParser().parsePathString(it.d).toPath() } }
    Canvas(Modifier.size(size)) {
        val s = minOf(this.size.width / ic.vb[2], this.size.height / ic.vb[3])
        val ox = (this.size.width - ic.vb[2] * s) / 2 - ic.vb[0] * s
        val oy = (this.size.height - ic.vb[3] * s) / 2 - ic.vb[1] * s
        withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
            paths.forEachIndexed { i, path ->
                if (ic.fillOnly || fill) drawPath(path, color)
                if (!ic.fillOnly) {
                    drawPath(path, color, style = Stroke(ic.p[i].sw ?: 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
private fun Lbl(t: String, size: TextUnit, color: Color, spacing: TextUnit = TextUnit.Unspecified) {
    Text(t, color = color, fontSize = size, lineHeight = size, maxLines = 1, letterSpacing = spacing)
}

/* ---------- клавиша ---------- */

private fun shade(c: Color, dark: Boolean, down: Boolean): Color = when {
    !down -> c
    dark -> Color((c.red * 1.35f).coerceAtMost(1f), (c.green * 1.35f).coerceAtMost(1f), (c.blue * 1.35f).coerceAtMost(1f), c.alpha)
    else -> Color(c.red * .88f, c.green * .88f, c.blue * .88f, c.alpha)
}

/** меню долгого нажатия: варианты символа (ударение и т.п.) и индекс выбранного по умолчанию */
private class MenuSpec(val items: List<String>, val def: Int)

private val LocalBend = compositionLocalOf { Animatable(0f) }

private val POP_H = 48.dp
private val POP_ITEM = 40.dp
private val POP_PAD = 4.dp
private val POP_TOP = (-56).dp

/** прозрачный запас над клавиатурой: всплывашки рисуются поверх приложения и не обрезаются */
const val HEADROOM_DP = 60

@Composable
private fun RowScope.Key(
    w: Float,
    bg: Color,
    pal: Pal,
    fly: String? = null,
    hint: String? = null,
    repeat: Boolean = false,
    sag: Boolean = false,
    menu: (() -> MenuSpec)? = null,
    onPick: ((String) -> Unit)? = null,
    onLong: (() -> Unit)? = null,
    onSwipe: ((Int) -> Unit)? = null,
    onTap: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var down by remember { mutableStateOf(false) }
    var popT by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<String>?>(null) }
    var sel by remember { mutableIntStateOf(0) }
    var keyLeft by remember { mutableFloatStateOf(0f) }
    var keyW by remember { mutableFloatStateOf(0f) }
    val dimS = LocalDim.current
    val bendA = LocalBend.current
    val scope = rememberCoroutineScope()
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLong)
    val swipe by rememberUpdatedState(onSwipe)
    val menuS by rememberUpdatedState(menu)
    val pickS by rememberUpdatedState(onPick)
    val flyS by rememberUpdatedState(fly)
    val hintS by rememberUpdatedState(hint)
    val prog = remember { Animatable(0f) }
    val view = LocalView.current
    val vibe = LocalVibe.current
    val spec: AnimationSpec<Float> = if (down) tween(70) else spring(0.45f, 700f)
    val sc by animateFloatAsState(if (down) (if (sag) 0.985f else 0.94f) else 1f, spec, label = "press")
    val scY by animateFloatAsState(if (down) (if (sag) 0.86f else 0.94f) else 1f, spec, label = "pressY")
    val dimmed = dimS.value && !down
    val popW = POP_ITEM * (items?.size ?: 0) + POP_PAD * 2

    Box(
        Modifier
            .weight(w)
            .fillMaxHeight()
            .zIndex(if (down) 4f else 0f)
            .onGloballyPositioned { keyLeft = it.positionInRoot().x; keyW = it.size.width.toFloat() }
            .graphicsLayer { scaleX = sc; scaleY = scY }
            .drawBehind {
                drawRoundRect(shade(bg, pal.dark, down), cornerRadius = CornerRadius(9.dp.toPx()))
            }
            .drawWithContent {
                drawContent()
                if (dimmed) drawRoundRect(Color.Black.copy(alpha = .5f), cornerRadius = CornerRadius(9.dp.toPx()))
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    d.consume()
                    down = true
                    if (vibe) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (flyS != null) scope.launch {
                        prog.snapTo(0f)
                        prog.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                    }
                    if (sag) scope.launch { bendA.animateTo(1f, spring(0.55f, 900f)) }
                    var fired = false
                    val job = scope.launch {
                        delay(380)
                        fired = true
                        val m = menuS?.invoke()
                        if (repeat) {
                            while (true) {
                                tap()
                                delay(55)
                            }
                        } else if (m != null && m.items.isNotEmpty()) {
                            sel = m.def
                            items = m.items
                            dimS.value = true
                        } else {
                            if (hintS != null) {
                                popT = hintS
                                dimS.value = true
                            }
                            long?.invoke()
                        }
                    }
                    var ok = false
                    try {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == d.id } ?: break
                            if (!ch.pressed) {
                                ok = !ch.isConsumed
                                break
                            }
                            if (ch.isConsumed) break
                            if (items == null && !fired &&
                                (ch.position.x !in 0f..size.width.toFloat() || ch.position.y !in 0f..size.height.toFloat())
                            ) break
                            val its = items
                            if (its != null) {
                                val itemPx = POP_ITEM.toPx()
                                val padPx = POP_PAD.toPx()
                                val left = (keyLeft + keyW / 2f - (sel.coerceIn(0, its.lastIndex) * itemPx + padPx + itemPx / 2f))
                                    .coerceIn(2.dp.toPx(), (view.width - its.size * itemPx - padPx * 2 - 2.dp.toPx()).coerceAtLeast(2.dp.toPx()))
                                val k = ((ch.position.x + keyLeft - left - padPx) / itemPx).toInt().coerceIn(0, its.lastIndex)
                                if (k != sel) {
                                    sel = k
                                    if (vibe) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                ch.consume()
                            }
                        }
                    } finally {
                        job.cancel()
                        down = false
                        popT = null
                        dimS.value = false
                        if (sag) scope.launch { bendA.animateTo(0f, spring(0.35f, 380f)) }
                    }
                    val its = items
                    items = null
                    if (its != null) {
                        its.getOrNull(sel)?.let { pickS?.invoke(it) }
                    } else if (ok && !fired) tap()
                }
            }
            .then(
                if (onSwipe == null) Modifier else Modifier.pointerInput(Unit) {
                    var acc = 0f
                    detectHorizontalDragGestures(onDragStart = { acc = 0f }) { change, dx ->
                        change.consume()
                        acc += dx
                        if (abs(acc) > 40.dp.toPx()) {
                            swipe?.invoke(if (acc > 0) -1 else 1)
                            if (vibe) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            acc = 0f
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (hint != null) {
            Text(
                hint, color = pal.hint, fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
            )
        }
        if (fly != null) {
            // буква плавно поднимается над клавишей и растворяется (полупрозрачная)
            Text(
                fly, color = pal.tx, fontSize = 20.sp,
                modifier = Modifier.graphicsLayer {
                    val p = prog.value
                    translationY = -8.dp.toPx() - 34.dp.toPx() * p
                    val s = 1f - .25f * p
                    scaleX = s
                    scaleY = s
                    alpha = if (p <= 0.001f || p >= 0.999f) 0f
                    else .5f * (1f - p).pow(1.4f) * (p / .08f).coerceAtMost(1f)
                },
            )
        }
        popT?.let { t ->
            // всплывашка с символом: «носик» снизу
            Box(Modifier.align(Alignment.TopCenter).offset(y = POP_TOP).requiredSize(POP_H)) {
                Box(
                    Modifier.align(Alignment.BottomCenter).offset(y = 5.dp).size(14.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .background(pal.pop, RoundedCornerShape(3.dp)),
                )
                Box(Modifier.fillMaxSize().background(pal.pop, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Text(t, color = pal.tx, fontSize = 26.sp)
                }
            }
        }
        items?.let { its ->
            // меню вариантов: ведём пальцем и отпускаем на нужном символе
            val itemPx = with(LocalDensity.current) { POP_ITEM.toPx() }
            val padPx = with(LocalDensity.current) { POP_PAD.toPx() }
            val edge = with(LocalDensity.current) { 2.dp.toPx() }
            val left = (keyLeft + keyW / 2f - (sel.coerceIn(0, its.lastIndex) * itemPx + padPx + itemPx / 2f))
            val maxLeft = (view.width - its.size * itemPx - padPx * 2 - edge).coerceAtLeast(edge)
            val dx = left.coerceIn(edge, maxLeft) - keyLeft
            Box(
                Modifier.align(Alignment.TopStart)
                    .offset { IntOffset(dx.roundToInt(), POP_TOP.roundToPx()) }
                    .requiredSize(popW, POP_H)
                    .background(pal.pop, RoundedCornerShape(18.dp))
                    .padding(POP_PAD),
            ) {
                Row {
                    its.forEachIndexed { i, t ->
                        Box(
                            Modifier.size(POP_ITEM)
                                .background(
                                    if (i == sel) pal.tx.copy(alpha = .16f) else Color.Transparent,
                                    RoundedCornerShape(14.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) { Text(t, color = pal.tx, fontSize = 24.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KRow(h: Dp, mb: Dp, gap: Dp = 4.5.dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(h), horizontalArrangement = Arrangement.spacedBy(gap), content = content)
    if (mb > 0.dp) Spacer(Modifier.height(mb))
}

@Composable
private fun SBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun suggest(l: Lang, w: String): List<String> {
    if (w.isEmpty()) return l.start
    val lw = w.lowercase()
    var r = l.dict.filter { it.startsWith(lw) && it != lw }.take(3)
    if (r.isEmpty()) r = listOf(w)
    val cap = w[0].isUpperCase()
    return r.map { if (cap) it.replaceFirstChar { c -> c.uppercaseChar() } else it }
}

/** shift: 0 выкл, 1 на одну букву, 2 caps lock; onBar — цвет системной полосы под клавиатурой */
@Composable
fun KeyboardRoot(
    cfg: Cfg, shift: Int, word: String, onShift: (Int) -> Unit, act: KbActions, onBar: (Int, Boolean) -> Unit,
) {
    val dark = when (cfg.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val pal = if (dark) DARK else LIGHT
    val a = KACC[ACC_MAP[cfg.accent.coerceIn(0, 5)]]
    val acc = if (dark) a.d else a.l
    val on = if (dark) a.dk else a.lo
    val accBg = if (dark) acc else lerp(acc, Color.White, .7f)
    val lang = LANGS[cfg.lang.coerceIn(0, LANGS.lastIndex)]
    var mode by remember { mutableIntStateOf(0) } // 0 буквы, 1 и 2 символы, 3 цифры
    val dim = remember { mutableStateOf(false) }
    val bend = remember { Animatable(0f) }
    val bgc = if (dim.value) lerp(pal.bg, Color.Black, .5f) else pal.bg
    val fly = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // системная полоса снизу затемняется вместе с клавиатурой при удержании клавиши
    SideEffect { onBar(bgc.toArgb(), dark) }

    CompositionLocalProvider(LocalVibe provides cfg.vibe, LocalDim provides dim, LocalBend provides bend) {
        Column(
            Modifier.fillMaxWidth()
                .padding(top = HEADROOM_DP.dp)
                .background(bgc, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 2.dp),
        ) {
            // верхняя панель: telegram, подсказки слов, буфер, микрофон
            Row(Modifier.fillMaxWidth().height(54.dp).padding(start = 4.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                SBtn(onClick = { scope.launch { fly.snapTo(0f); fly.animateTo(1f, tween(550)) } }) {
                    Box(
                        Modifier.graphicsLayer {
                            val p = fly.value
                            if (p > 0f && p < 1f) {
                                if (p < .4f) {
                                    val t = p / .4f
                                    translationX = 16.dp.toPx() * t; translationY = -16.dp.toPx() * t; alpha = 1f - t
                                } else if (p < .41f) {
                                    alpha = 0f
                                } else {
                                    val q = (p - .41f) / .59f
                                    translationX = -16.dp.toPx() * (1f - q); translationY = 16.dp.toPx() * (1f - q); alpha = q
                                }
                            }
                        },
                    ) { Ico(IC_TG, 24.dp, acc) }
                }
                Row(
                    Modifier.weight(1f).height(44.dp).padding(start = 4.dp).clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (mode != 3) {
                        suggest(lang, word).forEach { s ->
                            Text(
                                s, color = pal.tx, fontSize = 20.sp, maxLines = 1, softWrap = false,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                                ) { act.replaceWord(word, s) },
                            )
                        }
                    }
                }
                SBtn(onClick = {}) { Ico(IC_CLIP, 24.dp, pal.ic) }
                SBtn(onClick = {}) { Ico(IC_MIC, 24.dp, pal.ic) }
            }

            Column(
                Modifier.graphicsLayer {
                    val b = bend.value
                    transformOrigin = TransformOrigin(.5f, 1f)
                    translationY = 3.dp.toPx() * b
                    scaleY = 1f - .022f * b
                    scaleX = 1f + .006f * b
                },
            ) {
            when (mode) {
                0 -> {
                    lang.rows.forEachIndexed { ri, row ->
                        KRow(41.dp, 8.dp) {
                            if (ri == 1 && row.length < 11) Spacer(Modifier.weight(.5f))
                            if (ri == 2) {
                                Key(
                                    1.35f, if (shift > 0) accBg else pal.fn, pal,
                                    onLong = { onShift(2) }, onTap = { onShift(if (shift == 0) 1 else 0) },
                                ) { Ico(if (shift == 2) IC_LOCK else IC_SHIFT, 30.dp, if (shift > 0) on else pal.ic, fill = shift > 0) }
                            }
                            row.forEachIndexed { i, c ->
                                val ch = if (shift > 0) c.uppercase() else c.toString()
                                val h = if (cfg.hints) lang.hints[ri].getOrNull(i)?.toString() else null
                                Key(
                                    1f, pal.key, pal, fly = ch, hint = h,
                                    menu = if (cfg.hints) ({
                                        val (bf, af) = letterMenu(lang.name, c)
                                        val up = shift > 0
                                        val all = (bf + listOfNotNull(h) + af).map { if (up) it.uppercase() else it }
                                        MenuSpec(all, bf.size.coerceAtMost((all.size - 1).coerceAtLeast(0)))
                                    }) else null,
                                    onPick = { act.text(it) },
                                    onLong = { if (h != null) act.text(h) }, onTap = { act.text(ch) },
                                ) { Lbl(ch, 25.sp, pal.tx) }
                            }
                            if (ri == 2) {
                                Key(1.35f, pal.fn, pal, repeat = true, onTap = { act.backspace() }) { Ico(IC_BACK, 30.dp, pal.ic) }
                            }
                            if (ri == 1 && row.length < 11) Spacer(Modifier.weight(.5f))
                        }
                    }
                    KRow(46.dp, 0.dp) {
                        Key(127f, pal.fn, pal, onTap = { mode = 1 }) { Lbl("#1?", 20.sp, pal.tx) }
                        Key(84f, pal.key, pal, fly = ",", onTap = { act.text(",") }) { Lbl(",", 25.sp, pal.tx) }
                        Key(559f, pal.key, pal, sag = true, onLong = { act.switchLang() }, onSwipe = { act.switchLang(it) }, onTap = { act.text(" ") }) { SpaceLabel(lang.name, pal) }
                        Key(84f, pal.key, pal, fly = ".", onTap = { act.text(".") }) { Lbl(".", 25.sp, pal.tx) }
                        Key(162f, pal.ent, pal, onTap = { act.enter() }) { Ico(IC_ENTER, 32.dp, pal.ic) }
                    }
                }
                1, 2 -> {
                    val pg = mode - 1
                    SYMBOLS[pg].forEachIndexed { ri, row ->
                        KRow(41.dp, 8.dp) {
                            if (ri == 2) {
                                Key(1.16f, pal.fn, pal, onTap = { mode = if (mode == 1) 2 else 1 }) {
                                    Lbl(if (mode == 2) "1/2" else "=\\<", 20.sp, pal.tx)
                                }
                            }
                            row.forEachIndexed { i, c ->
                                val h = if (cfg.hints) SYMHINTS[pg][ri].getOrNull(i)?.toString() else null
                                Key(
                                    1f, pal.key, pal, fly = c.toString(), hint = h,
                                    onLong = { if (h != null) act.text(h) }, onTap = { act.text(c.toString()) },
                                ) { Lbl(c.toString(), 25.sp, pal.tx) }
                            }
                            if (ri == 2) {
                                Key(1.16f, pal.fn, pal, repeat = true, onTap = { act.backspace() }) { Ico(IC_BACK, 30.dp, pal.ic) }
                            }
                        }
                    }
                    KRow(46.dp, 0.dp) {
                        Key(106f, pal.fn, pal, onTap = { mode = 0 }) { Lbl("ABC", 20.sp, pal.tx) }
                        Key(87f, pal.fn, pal, onTap = { mode = 3 }) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { Lbl("1", 16.sp, pal.tx); Lbl("2", 16.sp, pal.tx) }
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { Lbl("3", 16.sp, pal.tx); Lbl("4", 16.sp, pal.tx) }
                            }
                        }
                        Key(70f, pal.key, pal, fly = ",", onTap = { act.text(",") }) { Lbl(",", 25.sp, pal.tx) }
                        Key(366f, pal.key, pal, sag = true, onLong = { act.switchLang() }, onSwipe = { act.switchLang(it) }, onTap = { act.text(" ") }) { SpaceLabel(lang.name, pal) }
                        Key(70f, pal.key, pal, fly = ".", onTap = { act.text(".") }) { Lbl(".", 25.sp, pal.tx) }
                        Key(134f, pal.ent, pal, onTap = { act.enter() }) { Ico(IC_ENTER, 32.dp, pal.ic) }
                    }
                }
                else -> {
                    val g = 5.5.dp
                    @Composable fun RowScope.Op(c: String, v: String = c) =
                        Key(1f, pal.key, pal, onTap = { act.text(v) }) { Lbl(c, 28.sp, pal.tx) }
                    @Composable fun RowScope.Big(c: String) =
                        Key(2.12f, pal.key, pal, onTap = { act.text(c) }) { Lbl(c, 36.sp, pal.tx) }
                    KRow(41.dp, 8.dp, g) { Op("+"); Big("1"); Big("2"); Big("3"); Op("−", "-") }
                    KRow(41.dp, 8.dp, g) {
                        Op("*"); Big("4"); Big("5"); Big("6")
                        Key(1f, pal.key, pal, onTap = { act.text(" ") }) { Ico(IC_SPACE, 28.dp, pal.ic) }
                    }
                    KRow(41.dp, 8.dp, g) {
                        Op("/"); Big("7"); Big("8"); Big("9")
                        Key(1f, pal.fn, pal, repeat = true, onTap = { act.backspace() }) { Ico(IC_BACK, 30.dp, pal.ic) }
                    }
                    KRow(46.dp, 0.dp, g) {
                        Key(1f, pal.fn, pal, onTap = { mode = 0 }) { Lbl("ABC", 20.sp, pal.tx) }
                        Row(Modifier.weight(2.12f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(g)) {
                            Key(1f, pal.fn, pal, onTap = { mode = 1 }) { Lbl("#1?", 20.sp, pal.tx) }
                            Op(",")
                        }
                        Big("0")
                        Row(Modifier.weight(2.12f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(g)) {
                            Op("."); Op("=")
                        }
                        Key(1f, pal.ent, pal, onTap = { act.enter() }) { Ico(IC_ENTER, 32.dp, pal.ic) }
                    }
                }
            }
            }

            // своей нижней панели нет: скрытие и смена клавиатуры — в системной панели
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BoxScope.SpaceLabel(name: String, pal: Pal) {
    Row(Modifier.padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(horizontal = 8.dp)) { Lbl("‹", 17.sp, pal.ic) }
        Lbl(name, 15.sp, pal.tx)
        Box(Modifier.padding(horizontal = 8.dp)) { Lbl("›", 17.sp, pal.ic) }
    }
    Box(Modifier.align(Alignment.CenterEnd).padding(end = 11.dp)) { Ico(IC_MIC, 20.dp, pal.ic) }
}
