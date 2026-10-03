package io.github.ieswar23.vibely.screenshots

import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.ieswar23.vibely.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real app (Hilt graph, Room, Paging, the mock Retrofit backend) on the JVM and captures
 * README screenshots into `docs/screenshots/`.
 *
 * Images are only written by `./gradlew recordRoborazziDebug`; a plain `testDebugUnitTest` still runs
 * every flow end to end (as a smoke test) but neither writes nor verifies images.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeRule)

    @Test
    fun homeFeed() {
        awaitHomeFeed()
        capture("01_home_feed")
    }

    @Test
    fun storyViewer() {
        awaitHomeFeed()
        // Story frames auto-advance on the compose clock, so drive it by hand to freeze mid-story.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText(STORY_USERNAME).performClick()
        pumpFramesUntil { composeRule.onAllNodesWithText(STORY_TEXT).exists() }
        // Let the enter transition finish and the first frame's progress bar fill to ~40%.
        composeRule.mainClock.advanceTimeBy(2_000)
        capture("02_story_viewer")
    }

    @Test
    fun comments() {
        awaitHomeFeed()
        composeRule.onAllNodesWithContentDescription("Comment").onFirst().performClick()
        composeRule.waitUntil(TIMEOUT) { composeRule.onAllNodesWithContentDescription("Like comment").fetchSemanticsNodes().size >= 3 }
        settle()
        capture("03_comments")
    }

    @Test
    fun createPost() {
        awaitHomeFeed()
        composeRule.onNodeWithContentDescription("Create").performClick()
        composeRule.waitUntil(TIMEOUT) { composeRule.onAllNodesWithText("New post").exists() }
        settle()
        composeRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput("Sunday chai and a good book ☕📚 #slowliving with @priya.wanders")
        settle()
        capture("04_create_post")
    }

    @Test
    fun explore() {
        awaitHomeFeed()
        composeRule.onNodeWithContentDescription("Explore").performClick()
        composeRule.waitUntil(TIMEOUT) { composeRule.onAllNodesWithText("Trending now").exists() }
        settle()
        capture("05_explore")
    }

    @Test
    fun profile() {
        awaitHomeFeed()
        composeRule.onNodeWithContentDescription("Profile").performClick()
        composeRule.waitUntil(TIMEOUT) { composeRule.onAllNodesWithContentDescription("Posts").exists() }
        settle()
        capture("06_profile")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeFeedDark() {
        awaitHomeFeed()
        // Scroll to a canvas post so the dark shot shows the gradient artwork.
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(3)
        settle()
        capture("07_home_feed_dark")
    }

    @Test
    fun pollFeed() {
        awaitHomeFeed()
        // The seeded monsoon-snack poll sits right after the first six posts (index 0 is the stories row).
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(POLL_LIST_INDEX)
        settle()
        composeRule.onNodeWithText(POLL_CHOICE).performClick()
        // The optimistic vote lands in Room, Paging re-emits and the card flips to animated results.
        composeRule.waitUntil(TIMEOUT) { composeRule.onAllNodesWithText(POLL_TOTAL_AFTER_VOTE, substring = true).exists() }
        settle()
        capture("08_poll_feed")
    }

    @Test
    fun createPoll() {
        awaitHomeFeed()
        composeRule.onNodeWithContentDescription("Create").performClick()
        composeRule.waitUntil(TIMEOUT) { composeRule.onAllNodesWithText("New post").exists() }
        settle()
        composeRule.onNodeWithText("Poll").performClick()
        settle()
        // Text fields in poll mode: question, the options, caption, location.
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Where should this weekend's monsoon trek be?")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("Rajmachi Fort")
        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("Kalsubai Peak")
        composeRule.onNodeWithText("3 days").performScrollTo().performClick()
        composeRule.onNodeWithText("Add option").performScrollTo().performClick()
        settle()
        composeRule.onAllNodes(hasSetTextAction())[3].performTextInput("Lohagad & Visapur")
        composeRule.onNodeWithText("Canvas").performScrollTo()
        settle()
        capture("09_create_poll")
    }

    /** Waits until both the stories row and the first feed page have come back from the mock API. */
    private fun awaitHomeFeed() {
        composeRule.waitUntil(TIMEOUT) {
            composeRule.onAllNodesWithText(STORY_USERNAME).exists() &&
                composeRule.onAllNodesWithContentDescription("Comment").exists()
        }
        settle()
    }

    /** With the compose clock paused, advances it frame by frame until [condition] holds. */
    private fun pumpFramesUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for the screen to load" }
            Thread.sleep(20)
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    /** Lets transitions and item animations finish (and any in-flight network response land). */
    private fun settle() {
        repeat(3) {
            Thread.sleep(300)
            composeRule.mainClock.advanceTimeBy(500)
            composeRule.waitForIdle()
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "$OUTPUT_DIR/$name.png",
            roborazziOptions = RoborazziOptions(
                recordOptions = RoborazziOptions.RecordOptions(resizeScale = RESIZE_SCALE),
            ),
        )
    }

    private fun SemanticsNodeInteractionCollection.exists() = fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val OUTPUT_DIR = "../docs/screenshots"

        /** 411dp at xxhdpi is 1233 px wide; scale to ~540 px to keep the PNGs small. */
        const val RESIZE_SCALE = 0.44
        const val TIMEOUT = 20_000L
        const val STORY_USERNAME = "meera.eats"
        const val STORY_TEXT = "Coffee #3 today"
        const val POLL_LIST_INDEX = 7
        const val POLL_CHOICE = "Pakoras with chai"
        const val POLL_TOTAL_AFTER_VOTE = "1,055 votes"
    }
}
