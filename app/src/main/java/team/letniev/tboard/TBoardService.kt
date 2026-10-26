package team.letniev.tboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class TBoardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val reg = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = reg
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry

    private val cfg = mutableStateOf(Cfg())
    private val shift = mutableIntStateOf(0)

    private val actions = object : KbActions {
        override fun text(s: String) {
            currentInputConnection?.commitText(s, 1)
            afterEdit()
        }

        override fun backspace() {
            val ic = currentInputConnection ?: return
            val sel = ic.getSelectedText(0)
            if (sel.isNullOrEmpty()) ic.deleteSurroundingText(1, 0) else ic.commitText("", 1)
            afterEdit()
        }

        override fun enter() {
            val ic = currentInputConnection ?: return
            val ei = currentInputEditorInfo
            val action = if (ei != null) ei.imeOptions and EditorInfo.IME_MASK_ACTION else EditorInfo.IME_ACTION_NONE
            val noAction = ei == null ||
                (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 ||
                action == EditorInfo.IME_ACTION_NONE ||
                action == EditorInfo.IME_ACTION_UNSPECIFIED
            if (noAction) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            } else {
                ic.performEditorAction(action)
            }
            afterEdit()
        }

        override fun switchLang() {
            val next = (cfg.value.lang + 1) % LANGS.size
            cfg.value = cfg.value.copy(lang = next)
            Prefs.save(this@TBoardService, cfg.value)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ssc.performAttach()
        ssc.performRestore(null)
        reg.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        cfg.value = Prefs.load(this)
    }

    override fun onCreateInputView(): View {
        val decor = window.window!!.decorView
        decor.setViewTreeLifecycleOwner(this)
        decor.setViewTreeViewModelStoreOwner(this)
        decor.setViewTreeSavedStateRegistryOwner(this)
        if (!reg.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            reg.handleLifecycleEvent(Lifecycle.Event.ON_START)
            reg.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        return ComposeView(this).apply {
            setContent {
                KeyboardRoot(
                    cfg = cfg.value,
                    shift = shift.intValue,
                    onShift = { shift.intValue = it },
                    act = actions,
                )
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        cfg.value = Prefs.load(this)
        shift.intValue = 0
        afterEdit()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun afterEdit() {
        if (shift.intValue == 2) return
        val ei = currentInputEditorInfo
        val caps = if (cfg.value.autocap && ei != null && ei.inputType != 0) {
            currentInputConnection?.getCursorCapsMode(ei.inputType) ?: 0
        } else 0
        shift.intValue = if (caps != 0) 1 else 0
    }

    override fun onDestroy() {
        reg.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
    }
}
