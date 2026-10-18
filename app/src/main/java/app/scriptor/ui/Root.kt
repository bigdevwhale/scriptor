package app.scriptor.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.scriptor.BuildConfig
import app.scriptor.R
import app.scriptor.container
import app.scriptor.data.Codex
import app.scriptor.data.Gloss
import app.scriptor.data.Page
import app.scriptor.data.Review
import app.scriptor.data.Settings
import app.scriptor.domain.Illumination
import app.scriptor.domain.Rules
import app.scriptor.notify.Hours
import app.scriptor.ui.chronicle.ChronicleTab
import app.scriptor.ui.codex.CloseScreen
import app.scriptor.ui.codex.CodexEditScreen
import app.scriptor.ui.codex.CodexScreen
import app.scriptor.ui.components.rememberNow
import app.scriptor.ui.desk.DeskTab
import app.scriptor.ui.desk.ErrataScreen
import app.scriptor.ui.library.LibraryTab
import app.scriptor.ui.onboarding.OnboardingScreen
import app.scriptor.ui.page.NoviceScreen
import app.scriptor.ui.page.PageEditScreen
import app.scriptor.ui.page.PageScreen
import app.scriptor.ui.restore.RestoreScreen
import app.scriptor.ui.settings.SettingsScreen
import app.scriptor.ui.theme.Nunito
import app.scriptor.ui.theme.U
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { DESK, LIBRARY, CHRONICLE }

/** Everything stored, plus what is derived from it, recomputed every minute or on change. */
@Immutable
class Desk(
    val codices: List<Codex>,
    val pages: List<Page>,
    val glosses: List<Gloss>,
    val reviews: List<Review>,
    val settings: Settings,
    val now: Long,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)
    val retention: Double get() = settings.retention.toDouble()
    private val byCodex = pages.groupBy { it.codexId }
    private val glossesBy = glosses.groupBy { it.pageId }
    private val reviewsBy = reviews.groupBy { it.pageId }

    fun codex(id: Long) = codices.firstOrNull { it.id == id }
    fun page(id: Long) = pages.firstOrNull { it.id == id }
    fun pagesOf(codexId: Long): List<Page> = byCodex[codexId].orEmpty().sortedBy { it.createdAt }
    fun glossesOf(pageId: Long): List<Gloss> = glossesBy[pageId].orEmpty()
    fun reviewsOf(pageId: Long): List<Review> = reviewsBy[pageId].orEmpty().sortedByDescending { it.at }
    fun illumination(p: Page): Illumination = Rules.illumination(p, glossesOf(p.id))

    val fading: List<Page> = Rules.fading(pages, now, retention)
    val yesterday: List<Page> = Rules.yesterday(pages, today, zone)
    val errata: List<Page> = pages.filter { it.erratum }
    val gilded: List<Page> = pages.filter { it.gilded }
    val open: List<Codex> = codices.filter { !it.closed }
    val closed: List<Codex> = codices.filter { it.closed }.sortedByDescending { it.closedAt }

    private fun day(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /** Matins is kept once a day: any review this morning of something written yesterday. */
    val matinsKept: Boolean = yesterday.isNotEmpty() && reviews.any { r -> day(r.at) == today && yesterday.any { it.id == r.pageId } }
    val workedDays: Set<LocalDate> = (reviews.map { day(it.at) } + pages.map { day(it.createdAt) }).toSet()
    val streak: Int = Rules.streak(workedDays, today)
    val restoredToday: Int = reviews.count { day(it.at) == today }
}

/** Where a restoration's pages come from. */
object Source {
    const val FADING = "fading"
    const val COMPLINE = "compline"
    const val MATINS = "matins"
    const val ERRATA = "errata"
    fun page(id: Long) = "page-$id"
    fun codex(id: Long) = "codex-$id"
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun newCodex() = nav.navigate("codex/new/0") { launchSingleTop = true }
    fun editCodex(id: Long) = nav.navigate("codex/new/$id") { launchSingleTop = true }
    fun codex(id: Long) = nav.navigate("codex/$id") { launchSingleTop = true }
    fun closeCodex(id: Long) = nav.navigate("close/$id") { launchSingleTop = true }
    fun write(codexId: Long, pageId: Long = 0) = nav.navigate("write/$codexId/$pageId") { launchSingleTop = true }
    fun page(id: Long) = nav.navigate("page/$id") { launchSingleTop = true }
    fun novice(pageId: Long) = nav.navigate("novice/$pageId") { launchSingleTop = true }
    fun restore(source: String) = nav.navigate("restore/$source") { launchSingleTop = true }
    fun errata() = nav.navigate("errata") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun back() = nav.popBackStack()

    /** After saving a new codex: straight into it, with the editor gone from the back stack. */
    fun replaceWithCodex(id: Long) = nav.navigate("codex/$id") { popUpTo("main") }
    fun replaceWithPage(id: Long) {
        nav.popBackStack()
        nav.navigate("page/$id") { launchSingleTop = true }
    }
}

@Composable
fun ScriptorRoot(settings: Settings, hourRequest: Pair<String, Int>? = null) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.DESK) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()

    val codices by dao.observeCodices().collectAsStateWithLifecycle(initialValue = null)
    val pages by dao.observePages().collectAsStateWithLifecycle(initialValue = null)
    val glosses by dao.observeGlosses().collectAsStateWithLifecycle(initialValue = null)
    val reviews by dao.observeReviews().collectAsStateWithLifecycle(initialValue = null)
    val tick = rememberNow(60_000)
    val desk = remember(codices, pages, glosses, reviews, settings, tick) {
        Desk(codices.orEmpty(), pages.orEmpty(), glosses.orEmpty(), reviews.orEmpty(), settings, System.currentTimeMillis())
    }
    val go = remember(nav) { Go(nav) }

    // A bell was tapped: straight into that hour.
    var handled by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(hourRequest, settings.onboarded) {
        val (hour, n) = hourRequest ?: return@LaunchedEffect
        if (n > handled && settings.onboarded) {
            handled = n
            Hours.clear(context)
            go.restore(if (hour == Hours.MATINS) Source.MATINS else Source.COMPLINE)
        }
    }

    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome.
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && nav.currentDestination?.route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }

    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(desk.pages.isNotEmpty()) {
        // Asked when it first matters: once there is a page that will fade.
        if (desk.pages.isNotEmpty() && (settings.compline || settings.matins) && Build.VERSION.SDK_INT >= 33 && !Hours.canNotify(context) && !BuildConfig.DEMO) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.background(U.c.bg),
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        composable("welcome") {
            OnboardingScreen {
                container.write { this.settings.update { it.copy(onboarded = true) } }
                nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                go.newCodex()
            }
        }
        composable("main") { MainTabs(tab, { tab = it }, go, desk) }
        composable("codex/new/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            CodexEditScreen(e.arguments?.getLong("id") ?: 0L, desk, go)
        }
        composable("codex/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            CodexScreen(e.arguments?.getLong("id") ?: 0L, desk, go)
        }
        composable("close/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            CloseScreen(e.arguments?.getLong("id") ?: 0L, desk, go)
        }
        composable(
            "write/{codex}/{page}",
            arguments = listOf(navArgument("codex") { type = NavType.LongType }, navArgument("page") { type = NavType.LongType }),
        ) { e ->
            PageEditScreen(e.arguments?.getLong("codex") ?: 0L, e.arguments?.getLong("page") ?: 0L, desk, go)
        }
        composable("page/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            PageScreen(e.arguments?.getLong("id") ?: 0L, desk, go)
        }
        composable("novice/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            NoviceScreen(e.arguments?.getLong("id") ?: 0L, desk, go)
        }
        composable("restore/{source}", arguments = listOf(navArgument("source") { type = NavType.StringType })) { e ->
            RestoreScreen(e.arguments?.getString("source").orEmpty(), desk, go)
        }
        composable("errata") { ErrataScreen(desk, go) }
        composable("settings") { SettingsScreen(desk, go) }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, desk: Desk) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.DESK -> DeskTab(desk, go)
                Tab.LIBRARY -> LibraryTab(desk, go)
                Tab.CHRONICLE -> ChronicleTab(desk, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(64.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.Edit, stringResource(R.string.tab_desk), tab == Tab.DESK) { onTab(Tab.DESK) }
            NavItem(Icons.Rounded.AutoStories, stringResource(R.string.tab_library), tab == Tab.LIBRARY) { onTab(Tab.LIBRARY) }
            NavItem(Icons.Rounded.HistoryEdu, stringResource(R.string.tab_chronicle), tab == Tab.CHRONICLE) { onTab(Tab.CHRONICLE) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.cinnabar else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.cinnabarSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Nunito, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
