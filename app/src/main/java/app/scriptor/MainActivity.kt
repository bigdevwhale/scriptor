package app.scriptor

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.scriptor.data.Settings
import app.scriptor.notify.Hours
import app.scriptor.ui.ScriptorRoot
import app.scriptor.ui.theme.ScriptorTheme

class MainActivity : AppCompatActivity() {
    /** The hour a bell asked for, with a counter so each tap on a notice opens it again. */
    private var hourRequest by mutableStateOf<Pair<String, Int>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        // Parchment: dark icons in the bars.
        val style = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            ScriptorTheme {
                // Wait for settings so the welcome doesn't flash for someone with a library.
                settings?.let { ScriptorRoot(it, hourRequest) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        val hour = Hours.hourOf(intent) ?: return
        hourRequest = hour to (hourRequest?.second ?: 0) + 1
    }
}
