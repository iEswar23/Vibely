package io.github.ieswar23.vibely.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.ieswar23.vibely.R
import io.github.ieswar23.vibely.ui.common.ProvideTickingNow
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.navigation.Routes
import io.github.ieswar23.vibely.ui.navigation.VibelyNavHost
import io.github.ieswar23.vibely.ui.theme.BrandBrush

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
)

private val tabs = listOf(
    TopLevelTab(Routes.HOME, R.string.nav_home),
    TopLevelTab(Routes.EXPLORE, R.string.nav_explore),
    TopLevelTab(Routes.CREATE, R.string.nav_create),
    TopLevelTab(Routes.ACTIVITY, R.string.nav_activity),
    TopLevelTab(Routes.MY_PROFILE, R.string.nav_profile),
)

@Composable
fun VibelyAppRoot(mainState: MainUiState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination?.route !in Routes.fullScreenRoutes

    ProvideTickingNow {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(),
                ) {
                    VibelyBottomBar(
                        destination = destination,
                        mainState = mainState,
                        onNavigate = { route -> navController.navigateToTab(route) },
                    )
                }
            },
        ) { padding ->
            val bottomPadding = PaddingValues(bottom = padding.calculateBottomPadding())
            VibelyNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottomPadding)
                    .consumeWindowInsets(bottomPadding),
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    if (route == Routes.CREATE) {
        navigate(Routes.CREATE) { launchSingleTop = true }
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun VibelyBottomBar(
    destination: NavDestination?,
    mainState: MainUiState,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.route == tab.route } == true
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                alwaysShowLabel = false,
                label = if (tab.route == Routes.CREATE) null else ({ Text(label) }),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                icon = {
                    when (tab.route) {
                        Routes.HOME -> Icon(if (selected) Icons.Rounded.Home else Icons.Outlined.Home, contentDescription = label)
                        Routes.EXPLORE -> Icon(if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore, contentDescription = label)
                        Routes.CREATE -> Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandBrush),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = label, tint = Color.White)
                        }
                        Routes.ACTIVITY -> BadgedBox(
                            badge = {
                                if (mainState.unreadActivity > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text(if (mainState.unreadActivity > 9) "9+" else mainState.unreadActivity.toString())
                                    }
                                }
                            },
                        ) {
                            Icon(if (selected) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = label)
                        }
                        else -> {
                            val me = mainState.currentUser
                            if (me != null) {
                                UserAvatar(
                                    name = me.name,
                                    seed = me.id,
                                    size = 28.dp,
                                    // Announce the tab ("Profile"), not the avatar's initials.
                                    modifier = Modifier
                                        .clearAndSetSemantics { contentDescription = label }
                                        .then(
                                            if (selected) {
                                                Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(BrandBrush)
                                                    .padding(2.dp)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            } else {
                                Icon(Icons.Rounded.Person, contentDescription = label)
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Shared transitions for the nav graph. */
internal object NavTransitions {
    val enter = slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260))
    val exit = fadeOut(tween(160))
    val popEnter = fadeIn(tween(220))
    val popExit = slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(220))
    val modalEnter = slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(300))
    val modalExit = slideOutVertically(tween(240)) { it / 3 } + fadeOut(tween(240))
}
