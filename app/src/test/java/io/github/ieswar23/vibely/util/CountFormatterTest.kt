package io.github.ieswar23.vibely.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountFormatterTest {

    @Test
    fun `small numbers are grouped`() {
        assertThat(CountFormatter.compact(987)).isEqualTo("987")
        assertThat(CountFormatter.compact(4_210)).isEqualTo("4,210")
    }

    @Test
    fun `large numbers are abbreviated without rounding up`() {
        assertThat(CountFormatter.compact(48_200)).isEqualTo("48.2K")
        assertThat(CountFormatter.compact(92_000)).isEqualTo("92K")
        assertThat(CountFormatter.compact(99_990)).isEqualTo("99.9K")
        assertThat(CountFormatter.compact(1_250_000)).isEqualTo("1.2M")
    }

    @Test
    fun `likes label handles singular`() {
        assertThat(CountFormatter.likes(1)).isEqualTo("1 like")
        assertThat(CountFormatter.likes(1_204)).isEqualTo("1,204 likes")
    }
}
