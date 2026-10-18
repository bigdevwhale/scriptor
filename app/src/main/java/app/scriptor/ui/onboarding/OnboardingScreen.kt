package app.scriptor.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.scriptor.R
import app.scriptor.ui.art.Initial
import app.scriptor.ui.art.Quill
import app.scriptor.ui.art.Seal
import app.scriptor.ui.art.Vines
import app.scriptor.ui.art.inkFor
import app.scriptor.ui.art.parchment
import app.scriptor.ui.components.Leaf
import app.scriptor.ui.components.PrimaryButton
import app.scriptor.ui.theme.U
import app.scriptor.ui.theme.UType

/** Three leaves: the scriptorium, the fading ink, the page that blooms. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().parchment(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_name).uppercase(), style = UType.Eyebrow, modifier = Modifier.align(Alignment.CenterHorizontally))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(step, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "leaf") { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (s) {
                            0 -> Quill(Modifier.size(150.dp))
                            1 -> FadingInk()
                            else -> Bloom()
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(listOf(R.string.welcome_1_title, R.string.welcome_2_title, R.string.welcome_3_title)[s]),
                        style = UType.Display, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(listOf(R.string.welcome_1_text, R.string.welcome_2_text, R.string.welcome_3_text)[s]),
                        style = UType.Sub.copy(textAlign = TextAlign.Center),
                    )
                }
            }
        }
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == step) U.c.cinnabar else U.c.line)) }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            stringResource(if (step < 2) R.string.next else R.string.welcome_start),
            { if (step < 2) step++ else onDone() }, Modifier.fillMaxWidth(), color = U.c.cinnabar,
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** A line of text breathing between fresh ink and almost gone. */
@Composable
private fun FadingInk() {
    val t by rememberInfiniteTransition(label = "ink").animateFloat(1f, 0.55f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "fade")
    Leaf(Modifier.width(280.dp)) {
        listOf(R.string.welcome_ink_1, R.string.welcome_ink_2, R.string.welcome_ink_3).forEachIndexed { i, id ->
            Text(stringResource(id), style = UType.Script.copy(color = inkFor(U.c, (t - i * 0.08f).toDouble())))
        }
    }
}

/** The same page, illuminated: initial, vines, the seal. */
@Composable
private fun Bloom() {
    Box(Modifier.size(width = 240.dp, height = 190.dp)) {
        Leaf(Modifier.fillMaxSize(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Row(Modifier.fillMaxSize()) {
                Vines(Modifier.width(26.dp).fillMaxSize().padding(vertical = 8.dp))
                Column(Modifier.padding(14.dp)) {
                    Initial('S', 58.dp, painted = true, gilded = true)
                    Spacer(Modifier.height(10.dp))
                    repeat(4) { i ->
                        Box(Modifier.padding(vertical = 4.dp).height(5.dp).fillMaxWidth(if (i == 3) 0.6f else 1f).clip(CircleShape).background(U.c.ink.copy(alpha = 0.7f)))
                    }
                }
            }
        }
        Seal(56.dp, Modifier.align(Alignment.BottomEnd).padding(6.dp))
    }
}
