package io.github.ieswar23.vibely.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Current time, refreshed every 30 seconds so "time ago" labels stay accurate on screen. */
val LocalNow = compositionLocalOf { System.currentTimeMillis() }

@Composable
fun ProvideTickingNow(content: @Composable () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    CompositionLocalProvider(LocalNow provides now, content = content)
}
