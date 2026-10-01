package io.github.ieswar23.vibely.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextTokenParserTest {

    @Test
    fun `splits plain text hashtags and mentions in order`() {
        val tokens = TextTokenParser.parse("Sunrise with @priya.wanders #Travel!")

        assertThat(tokens).containsExactly(
            TextToken.Plain("Sunrise with "),
            TextToken.Mention("@priya.wanders", "priya.wanders"),
            TextToken.Plain(" "),
            TextToken.Hashtag("#Travel", "travel"),
            TextToken.Plain("!"),
        ).inOrder()
    }

    @Test
    fun `tokens reassemble to the original text`() {
        val text = "Ramen night 🍜 #ramen #homecooking with @emma.bakes. Thanks!"
        assertThat(TextTokenParser.parse(text).joinToString("") { it.text }).isEqualTo(text)
    }

    @Test
    fun `email addresses are not mentions`() {
        val tokens = TextTokenParser.parse("Mail hello@vibely.app for access")
        assertThat(tokens.filterIsInstance<TextToken.Mention>()).isEmpty()
    }

    @Test
    fun `mentions do not swallow a trailing full stop`() {
        assertThat(TextTokenParser.mentions("Shoutout @kabir.codes.")).containsExactly("kabir.codes")
    }

    @Test
    fun `numeric only and mid-word hashes are ignored`() {
        assertThat(TextTokenParser.hashtags("Room #1 and C#sharp and issue#42")).isEmpty()
        assertThat(TextTokenParser.hashtags("#100daysofpractice")).containsExactly("100daysofpractice")
    }

    @Test
    fun `hashtags are lower-cased and de-duplicated`() {
        assertThat(TextTokenParser.hashtags("#Travel #travel #TRAVEL #food")).containsExactly("travel", "food").inOrder()
    }

    @Test
    fun `empty text yields no tokens`() {
        assertThat(TextTokenParser.parse("")).isEmpty()
    }
}
