package team.letniev.tboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SettingsScreen() }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp))
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(Prefs.load(ctx)) }
    fun upd(n: Cfg) {
        cfg = n
        Prefs.save(ctx, n)
    }

    val dark = when (cfg.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val acc = ACCENTS[cfg.accent]
    val scheme = if (dark) darkColorScheme(primary = acc) else lightColorScheme(primary = acc)

    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("TBoard", fontSize = 22.sp, modifier = Modifier.padding(start = 6.dp, top = 12.dp))

                Section("Подключение") {
                    Button(
                        onClick = { ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("1. Включить клавиатуру") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("2. Выбрать TBoard") }
                }

                Section("Оформление") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Авто", "Светлая", "Тёмная").forEachIndexed { i, t ->
                            FilterChip(selected = cfg.theme == i, onClick = { upd(cfg.copy(theme = i)) }, label = { Text(t) })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ACCENTS.forEachIndexed { i, c ->
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(c).clickable { upd(cfg.copy(accent = i)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cfg.accent == i) Text("✓", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Section("Ввод") {
                    SwitchRow("Вибрация", "Короткий отклик на каждую клавишу", cfg.vibe) { upd(cfg.copy(vibe = it)) }
                    SwitchRow("Символы на клавишах", "В углу; вставляются долгим нажатием", cfg.hints) { upd(cfg.copy(hints = it)) }
                    SwitchRow("Автозаглавная буква", "Начало предложения с большой", cfg.autocap) { upd(cfg.copy(autocap = it)) }
                }

                Section("Проверка") {
                    var t by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = t, onValueChange = { t = it },
                        label = { Text("Напиши что-нибудь") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
