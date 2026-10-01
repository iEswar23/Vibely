package io.github.ieswar23.vibely.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.ieswar23.vibely.ui.NavTransitions
import io.github.ieswar23.vibely.ui.activity.ActivityScreen
import io.github.ieswar23.vibely.ui.comments.CommentsScreen
import io.github.ieswar23.vibely.ui.create.CreatePostScreen
import io.github.ieswar23.vibely.ui.explore.ExploreScreen
import io.github.ieswar23.vibely.ui.explore.HashtagScreen
import io.github.ieswar23.vibely.ui.feed.FeedScreen
import io.github.ieswar23.vibely.ui.post.PostDetailScreen
import io.github.ieswar23.vibely.ui.profile.EditProfileScreen
import io.github.ieswar23.vibely.ui.profile.ProfileScreen
import io.github.ieswar23.vibely.ui.settings.SettingsScreen
import io.github.ieswar23.vibely.ui.story.StoryViewerScreen

/** Navigation callbacks shared by every screen that renders posts or users. */
class AppNavigator(private val navController: NavHostController) {
    /**
     * Pops the current screen, ignoring repeated calls while a transition is already running
     * (e.g. a story finishing at the same moment the user swipes it away).
     */
    fun back() {
        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
            navController.popBackStack()
        }
    }
    fun profile(userId: String) = navController.navigate(Routes.profile(userId))
    fun mention(username: String) = navController.navigate(Routes.profile("@$username"))
    fun post(postId: String) = navController.navigate(Routes.post(postId))
    fun comments(postId: String) = navController.navigate(Routes.comments(postId))
    fun story(userId: String) = navController.navigate(Routes.story(userId))
    fun hashtag(tag: String) = navController.navigate(Routes.hashtag(tag))
    fun create() = navController.navigate(Routes.CREATE) { launchSingleTop = true }
    fun settings() = navController.navigate(Routes.SETTINGS)
    fun editProfile() = navController.navigate(Routes.EDIT_PROFILE)

    /** After publishing or resetting, return to a fresh home feed. */
    fun home() = navController.navigate(Routes.HOME) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun VibelyNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val navigator = remember(navController) { AppNavigator(navController) }
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { NavTransitions.enter },
        exitTransition = { NavTransitions.exit },
        popEnterTransition = { NavTransitions.popEnter },
        popExitTransition = { NavTransitions.popExit },
    ) {
        composable(Routes.HOME) {
            FeedScreen(navigator = navigator)
        }
        composable(Routes.EXPLORE) {
            ExploreScreen(navigator = navigator)
        }
        composable(
            Routes.CREATE,
            enterTransition = { NavTransitions.modalEnter },
            popExitTransition = { NavTransitions.modalExit },
        ) {
            CreatePostScreen(onClose = { navigator.back() }, onPublished = { navigator.home() })
        }
        composable(Routes.ACTIVITY) {
            ActivityScreen(navigator = navigator)
        }
        composable(Routes.MY_PROFILE) {
            ProfileScreen(navigator = navigator, showBack = false)
        }
        composable(
            Routes.PROFILE,
            arguments = listOf(navArgument(Routes.ARG_USER_KEY) { type = NavType.StringType }),
        ) {
            ProfileScreen(navigator = navigator)
        }
        composable(
            Routes.POST,
            arguments = listOf(navArgument(Routes.ARG_POST_ID) { type = NavType.StringType }),
        ) {
            PostDetailScreen(navigator = navigator)
        }
        composable(
            Routes.COMMENTS,
            arguments = listOf(navArgument(Routes.ARG_POST_ID) { type = NavType.StringType }),
            enterTransition = { NavTransitions.modalEnter },
            popExitTransition = { NavTransitions.modalExit },
        ) {
            CommentsScreen(navigator = navigator)
        }
        composable(
            Routes.STORY,
            arguments = listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.StringType }),
            enterTransition = { NavTransitions.modalEnter },
            popExitTransition = { NavTransitions.modalExit },
        ) {
            StoryViewerScreen(onClose = { navigator.back() }, onOpenProfile = navigator::profile)
        }
        composable(
            Routes.HASHTAG,
            arguments = listOf(navArgument(Routes.ARG_TAG) { type = NavType.StringType }),
        ) {
            HashtagScreen(navigator = navigator)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navigator.back() }, onResetComplete = { navigator.home() })
        }
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(onBack = { navigator.back() })
        }
    }
}
