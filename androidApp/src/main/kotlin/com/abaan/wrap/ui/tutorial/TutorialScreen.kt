package com.abaan.wrap.ui.tutorial

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.abaan.wrap.R
import kotlinx.coroutines.launch

/** Remembers whether the tutorial has been shown, so it only opens by itself on first launch. */
class Onboarding(context: Context) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    val tutorialSeen: Boolean get() = prefs.getBoolean(KEY_SEEN, false)

    fun markTutorialSeen() = prefs.edit { putBoolean(KEY_SEEN, true) }

    private companion object {
        const val KEY_SEEN = "tutorial_seen"
    }
}

private data class TutorialPage(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    TutorialPage(R.drawable.ic_launcher_foreground, R.string.tutorial_welcome_title, R.string.tutorial_welcome_body),
    TutorialPage(R.drawable.ic_schedule, R.string.tutorial_jobs_title, R.string.tutorial_jobs_body),
    TutorialPage(R.drawable.ic_directions, R.string.tutorial_day_title, R.string.tutorial_day_body),
    TutorialPage(R.drawable.ic_payments, R.string.tutorial_paid_title, R.string.tutorial_paid_body),
    TutorialPage(R.drawable.ic_receipt, R.string.tutorial_spend_title, R.string.tutorial_spend_body),
    TutorialPage(R.drawable.ic_report, R.string.tutorial_reports_title, R.string.tutorial_reports_body),
    TutorialPage(R.drawable.ic_lock, R.string.tutorial_safe_title, R.string.tutorial_safe_body),
)

/** A short swipe-through guide to the app. Shown on first launch and from Settings. */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                if (!last) TextButton(onClick = onDone) { Text(stringResource(R.string.tutorial_skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    ) {
                        if (index == 0) {
                            // The app icon keeps its own colours.
                            Image(painterResource(page.icon), contentDescription = null, modifier = Modifier.size(112.dp).clip(CircleShape))
                        } else {
                            Icon(
                                painterResource(page.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(52.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            val progress = stringResource(R.string.tutorial_progress, pager.currentPage + 1, pages.size)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics { contentDescription = progress },
            ) {
                pages.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(if (index == pager.currentPage) 10.dp else 8.dp)
                            .background(
                                if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            ),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (pager.currentPage > 0) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                        Text(stringResource(R.string.tutorial_back))
                    }
                }
                Spacer(Modifier.weight(1f).width(0.dp))
                Button(onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Text(stringResource(if (last) R.string.tutorial_start else R.string.tutorial_next))
                }
            }
        }
    }
}
