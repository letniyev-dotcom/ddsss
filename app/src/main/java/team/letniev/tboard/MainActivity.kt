package team.letniev.tboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as dpLerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsScreen() }
    }
}

/* ---------- палитра экрана (токены из HTML-прототипа) ---------- */

private class Ui(
    val dark: Boolean, val page: Color, val card: Color, val line: Color, val soff: Color,
    val tx: Color, val t2: Color, val mute: Color, val dng: Color, val acc: Color, val on: Color,
)

private val LocalUi = compositionLocalOf<Ui> { error("Ui не задан") }

private val ACC_NAMES = listOf("Синий", "Бирюзовый", "Зелёный", "Оранжевый", "Розовый", "Фиолетовый")

private fun ui(dark: Boolean, acc: Color, on: Color) =
    if (dark) {
        Ui(true, Color(0xFF121316), Color(0xFF1D1F26), Color(0xFF292C35), Color(0xFF3A3D48), Color(0xFFE6E6EA), Color(0xFFA3A7B6), Color(0xFF7C808C), Color(0xFFFFB4AB), acc, on)
    } else {
        Ui(false, Color(0xFFF6F7FA), Color(0xFFFFFFFF), Color(0xFFECEEF3), Color(0xFFDCDFE7), Color(0xFF1B1B1F), Color(0xFF5F636D), Color(0xFF8B8F99), Color(0xFFBA1A1A), acc, on)
    }

/* ---------- кирпичики ---------- */

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val u = LocalUi.current
    Text(
        title, color = u.acc, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(u.card)) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.padding(start = 56.dp, end = 16.dp).fillMaxWidth().height(1.dp).background(LocalUi.current.line))
}

@Composable
private fun Tgl(on: Boolean) {
    val u = LocalUi.current
    val p by animateFloatAsState(if (on) 1f else 0f, tween(180), label = "tgl")
    Box(Modifier.size(46.dp, 28.dp).clip(RoundedCornerShape(14.dp)).background(lerp(u.soff, u.acc, p))) {
        Box(
            Modifier.offset(dpLerp(7.dp, 22.dp, p), dpLerp(7.dp, 4.dp, p))
                .size(dpLerp(14.dp, 20.dp, p)).clip(CircleShape).background(lerp(u.mute, u.on, p)),
        )
    }
}

@Composable
private fun Item(
    icon: Ic, title: String, sub: String? = null, danger: Boolean = false, enabled: Boolean = true,
    onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null,
) {
    val u = LocalUi.current
    Row(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else .45f)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 60.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Ico(icon, 24.dp, if (danger) u.dng else u.acc)
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) u.dng else u.tx, fontSize = 15.sp, lineHeight = 20.sp)
            if (sub != null) Text(sub, color = u.t2, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
        trailing?.invoke()
    }
}

/** карточка темы: мини-превью клавиатуры в выбранном цвете */
@Composable
private fun ThemeCard(
    label: String, selected: Boolean, accent: Color, page: Color, key: Color, kb: Color, tx: Color,
    onClick: () -> Unit, custom: (@Composable () -> Unit)? = null,
) {
    val u = LocalUi.current
    Column(
        Modifier.width(86.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.border(2.5.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(21.dp)).padding(3.dp),
        ) {
            val pill = RoundedCornerShape(11.dp)
            Box(
                Modifier.size(80.dp, 124.dp).clip(RoundedCornerShape(18.dp)).background(page)
                    .border(1.dp, tx.copy(alpha = .09f), RoundedCornerShape(18.dp)),
            ) {
                if (custom != null) {
                    custom()
                } else {
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 10.dp).size(50.dp, 22.dp).background(accent, pill))
                    Box(
                        Modifier.align(Alignment.TopStart).padding(top = 44.dp, start = 10.dp).size(42.dp, 20.dp)
                            .background(key, RoundedCornerShape(10.dp))
                            .border(1.dp, tx.copy(alpha = .08f), RoundedCornerShape(10.dp)),
                    )
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(48.dp).background(kb)
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(6) { Box(Modifier.weight(1f).height(9.dp).background(key, RoundedCornerShape(3.dp))) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(3) { Box(Modifier.weight(1f).height(9.dp).background(key, RoundedCornerShape(3.dp))) }
                            Box(Modifier.weight(3f).height(9.dp).alpha(.85f).background(accent, RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
        }
        Text(
            label, color = if (selected) u.acc else u.t2, fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}


private fun fileName(ctx: Context, uri: android.net.Uri): String {
    val n = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
    return (n ?: "Telegram").replace(Regex("\\.attheme$", RegexOption.IGNORE_CASE), "").replace('_', ' ')
}

/* ---------- экран ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(Prefs.load(ctx)) }
    fun upd(n: Cfg) {
        cfg = n
        Prefs.save(ctx, n)
    }

    val sysDark = isSystemInDarkTheme()
    val dark = when (cfg.theme) {
        1 -> false
        2 -> true
        else -> sysDark
    }
    val a = KACC[cfg.accent.coerceIn(0, KACC.lastIndex)]
    val acc = if (dark) a.d else a.l
    val on = if (dark) a.dk else a.lo
    val u = ui(dark, acc, on)
    val tg = cfg.tg
    val tgUse = cfg.tgUse && tg != null

    // прозрачные системные панели, иконки по теме
    val view = LocalView.current
    SideEffect {
        val w = (view.context as Activity).window
        w.statusBarColor = android.graphics.Color.TRANSPARENT
        w.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            w.isNavigationBarContrastEnforced = false
            w.isStatusBarContrastEnforced = false
        }
        val c = WindowCompat.getInsetsController(w, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }

    val pickTheme = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val th = try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) null else TgThemes.derive(TgThemes.parse(bytes), fileName(ctx, uri))
            } catch (e: Exception) {
                null
            }
            if (th == null) {
                Toast.makeText(ctx, "Не похоже на файл темы Telegram", Toast.LENGTH_SHORT).show()
            } else {
                upd(cfg.copy(tg = th, tgUse = true))
                Toast.makeText(ctx, "Тема «${th.name}» применена", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // проверка клавиатуры без поля ввода: невидимый EditText ловит ввод, текст показывается плашкой
    val typed = remember { mutableStateOf("") }
    val editRef = remember { arrayOfNulls<EditText>(1) }
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    val imeVisible = WindowInsets.isImeVisible
    fun hideKb() {
        editRef[0]?.let {
            it.clearFocus()
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
    fun showKb() {
        val def = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        if (!def.startsWith(ctx.packageName)) {
            imm.showInputMethodPicker()
            return
        }
        editRef[0]?.let {
            it.requestFocus()
            imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    LaunchedEffect(imeVisible) {
        if (!imeVisible) {
            editRef[0]?.let {
                it.setText("")
                it.clearFocus()
            }
            typed.value = ""
        }
    }

    val scheme = if (dark) darkColorScheme(primary = acc) else lightColorScheme(primary = acc)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalUi provides u, LocalContentColor provides u.tx) {
            Box(Modifier.fillMaxSize().background(u.page)) {
                Column(
                    Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { hideKb() },
                ) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Spacer(Modifier.height(52.dp))

                    // шапка: логотип (потом заменим на картинку с названием), название, описание
                    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(30.dp)).background(u.acc), contentAlignment = Alignment.Center) {
                            Ico(IC_KBD, 54.dp, u.on)
                        }
                        Text(
                            "Клавиатура", color = u.tx, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 20.dp),
                        )
                        Text(
                            "Быстрый набор с подсказками слов и символами на клавишах. Цвета можно взять из темы Telegram.",
                            color = u.t2, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp).widthIn(max = 300.dp),
                        )
                    }

                    Section("Подключение") {
                        Item(AppIcons.tog, "Включить клавиатуру", "Откроет системные настройки", onClick = {
                            ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        })
                        Divider()
                        Item(IC_KBD, "Выбрать клавиатуру", "Сделать основной для ввода", onClick = { imm.showInputMethodPicker() })
                    }

                    Section("Оформление") {
                        val kp = if (dark) Triple(Color(0xFF121316), Color(0xFF151A2D), Color(0xFF2C2F3C)) else Triple(Color(0xFFF6F7FA), Color(0xFFE7EAF2), Color(0xFFFFFFFF))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 13.dp, end = 13.dp, top = 15.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            KACC.forEachIndexed { i, k ->
                                val c = if (dark) k.d else k.l
                                ThemeCard(
                                    ACC_NAMES[i], !tgUse && i == cfg.accent, c, kp.first, kp.third, kp.second, u.tx,
                                    onClick = { upd(cfg.copy(accent = i, tgUse = false)) },
                                )
                            }
                            if (tg != null) {
                                ThemeCard(
                                    tg.name, tgUse, Color(tg.acc), Color(tg.bg), Color(tg.key), Color(tg.bg), Color(tg.tx),
                                    onClick = { upd(cfg.copy(tgUse = !cfg.tgUse)) },
                                )
                            } else {
                                ThemeCard(
                                    "Из Telegram", false, u.acc, u.line, u.line, u.line, u.tx,
                                    onClick = { pickTheme.launch("*/*") },
                                    custom = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ico(IC_TG, 34.dp, u.acc) } },
                                )
                            }
                        }
                        Item(
                            if (dark) AppIcons.moon else AppIcons.sun,
                            if (dark) "Тёмная тема" else "Светлая тема",
                            "Нажми, чтобы включить ${if (dark) "светлую" else "тёмную"}",
                            onClick = { upd(cfg.copy(theme = if (dark) 1 else 2)) },
                        )
                    }

                    Section("Тема из Telegram") {
                        Item(IC_TG, if (tg != null) "Заменить тему" else "Применить из Telegram", if (tg != null) tg.name else "Файл .attheme", onClick = {
                            pickTheme.launch("*/*")
                        })
                        Divider()
                        Item(
                            IC_TG, "Только в Telegram", "В других приложениях обычная",
                            enabled = tgUse, onClick = { upd(cfg.copy(tgOnly = !cfg.tgOnly)) }, trailing = { Tgl(cfg.tgOnly) },
                        )
                        if (tg != null) {
                            Divider()
                            Item(AppIcons.trash, "Убрать тему", danger = true, onClick = { upd(cfg.copy(tg = null, tgUse = false)) })
                        }
                    }

                    Section("Ввод") {
                        Item(AppIcons.words, "Подсказки слов", "Слова над клавишами", onClick = { upd(cfg.copy(sugg = !cfg.sugg)) }, trailing = { Tgl(cfg.sugg) })
                        Divider()
                        Item(AppIcons.hint, "Символы на клавишах", "В углу, долгое нажатие", onClick = { upd(cfg.copy(hints = !cfg.hints)) }, trailing = { Tgl(cfg.hints) })
                        Divider()
                        Item(AppIcons.caps, "Автозаглавная буква", "Начало предложения с большой", onClick = { upd(cfg.copy(autocap = !cfg.autocap)) }, trailing = { Tgl(cfg.autocap) })
                        Divider()
                        Item(AppIcons.vib, "Вибрация при нажатии", "Короткий отклик на каждую клавишу", onClick = { upd(cfg.copy(vibe = !cfg.vibe)) }, trailing = { Tgl(cfg.vibe) })
                    }

                    Text(
                        "letniev team · версия 0.1", color = u.mute, fontSize = 12.sp, lineHeight = 16.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                    )
                    Spacer(Modifier.height(112.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }

                // мягкое растворение контента под прозрачным статус-баром
                val sb = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().height(sb + 14.dp)
                        .background(Brush.verticalGradient(listOf(u.page, u.page.copy(alpha = 0f)))),
                )

                // невидимое поле, которому клавиатура отдаёт ввод
                AndroidView(
                    modifier = Modifier.align(Alignment.TopStart).size(1.dp),
                    factory = { c ->
                        EditText(c).apply {
                            background = null
                            alpha = 0f
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
                                override fun onTextChanged(s: CharSequence?, st: Int, bef: Int, cnt: Int) {}
                                override fun afterTextChanged(s: Editable?) {
                                    typed.value = s?.toString() ?: ""
                                }
                            })
                            editRef[0] = this
                        }
                    },
                )

                // плашка с набранным текстом над клавиатурой
                if (imeVisible && typed.value.isNotEmpty()) {
                    val t = typed.value.replace('\n', ' ').let { if (it.length > 34) "…" + it.takeLast(33) else it }
                    Box(
                        Modifier.align(Alignment.BottomCenter).imePadding().padding(bottom = 12.dp).height(44.dp)
                            .clip(RoundedCornerShape(22.dp)).background(u.tx).padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(t, color = u.page, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }

                // остров внизу: показать клавиатуру
                AnimatedVisibility(
                    visible = !imeVisible,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 26.dp),
                    enter = fadeIn(tween(250)) + scaleIn(tween(350), initialScale = .6f),
                    exit = fadeOut(tween(150)) + scaleOut(tween(200), targetScale = .6f),
                ) {
                    Row(
                        Modifier.height(54.dp).clip(RoundedCornerShape(27.dp)).background(u.tx)
                            .clickable { showKb() }.padding(start = 18.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Ico(IC_KBD, 24.dp, u.page)
                        Text("Показать клавиатуру", color = u.page, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}
