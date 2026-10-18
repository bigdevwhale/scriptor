package app.scriptor

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import app.scriptor.data.DemoSeed
import app.scriptor.data.Settings
import app.scriptor.ui.ScriptorRoot
import app.scriptor.ui.theme.ScriptorTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the main screens with the demo scriptorium to PNGs (run with `-Prender [-Plang=ru]`): a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class RenderScreens {
    @get:Rule
    val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val lang = System.getProperty("render.lang") ?: "en"
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() }
    private val ru = lang == "ru"

    private fun settle(ms: Long = 3000) {
        rule.mainClock.autoAdvance = false
        var t = 0L
        while (t < ms) {
            Thread.sleep(40)
            rule.mainClock.advanceTimeBy(100)
            t += 100
        }
    }

    private fun shot(name: String) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        rule.mainClock.advanceTimeByFrame()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val root = rule.activity.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(b))
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String, ms: Long = 2000, exact: Boolean = false) {
        settle(500)
        val n = rule.onAllNodesWithText(text, substring = !exact, useUnmergedTree = exact)[0]
        runCatching { n.performScrollTo() }
        n.performClick()
        settle(ms)
    }

    private fun back() {
        rule.onAllNodesWithContentDescription(if (ru) "Назад" else "Back")[0].performClick()
        settle(1200)
    }

    private fun t(en: String, ru: String) = if (this.ru) ru else en

    @Test
    fun screens() {
        if (ru) RuntimeEnvironment.setQualifiers("+ru")
        val app = ApplicationProvider.getApplicationContext<ScriptorApp>()
        runBlocking { DemoSeed.seedIfEmpty(app, app.container) }
        rule.setContent {
            val s: Settings? by app.container.settings.settings.collectAsState(initial = null)
            ScriptorTheme { s?.let { ScriptorRoot(it) } }
        }
        settle(4000)
        shot("1-desk")
        tap(t("Library", "Библиотека"), exact = true)
        shot("2-library")
        tap(t("Make It Stick", "Запомнить всё"))
        shot("3-codex")
        tap(t("Recall beats rereading", "Вспоминание сильнее"))
        shot("4-page")
        back()
        tap(t("Mix the topics", "Перемешивай темы"))
        shot("5-page-fading")
        tap(t("Restore from memory", "Восстановить по памяти"))
        shot("6-restore")
        tap(t("recalled it in my head", "Вспомнил в уме"))
        shot("7-stake")
        tap(t("Certain", "Уверен"), exact = true)
        shot("8-reveal")
        back()
        tap(t("Explain to the novice", "Объяснить послушнику"))
        shot("9-novice")
        back()
        back()
        back()
        tap(t("Chronicle", "Летопись"), exact = true)
        shot("10-chronicle")
        tap(t("Crafts open", "Открыто ремёсел"))
        shot("11-crafts")
        back()
        tap(t("Library", "Библиотека"), exact = true)
        tap(t("Monastery of memory", "Монастырь памяти"))
        shot("12-palace")
        tap(t("Walk through the monastery", "Пройтись по монастырю"))
        shot("13-walk")
        tap(t("Look", "Посмотреть"), exact = true)
        shot("14-walk-look")
        back()
        back()
        tap(t("How the Bra", "Как мозг за"))
        tap(t("The forgetting curve", "Кривая забывания"))
        shot("15-miniature")
        back()
        back()
        tap(t("Desk", "Стол"), exact = true)
        shot("16-desk-before-exam")
        tap(t("Questions from every codex", "Вопросы из всех рукописей"))
        shot("17-exam")
        back()
        rule.onAllNodesWithContentDescription(t("Settings", "Настройки"))[0].performClick()
        settle(1500)
        shot("18-settings")
    }
}
