package io.github.ieswar23.vibely.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** The violet → pink brand gradient used for accents, story rings and primary buttons. */
val BrandGradientColors = listOf(Violet600, Pink500)
val BrandBrush: Brush = Brush.linearGradient(BrandGradientColors)

/** Story ring for unseen stories: brand gradient with a warm orange tail. */
val StoryRingBrush: Brush = Brush.sweepGradient(listOf(Violet600, Pink500, Orange400, Pink500, Violet600))

data class CanvasGradient(val key: String, val label: String, val colors: List<Color>) {
    val brush: Brush
        get() = Brush.linearGradient(colors, start = Offset.Zero, end = Offset.Infinite)
}

/** Palette for post and story "canvases". Keys are shared with the backend fixtures. */
object CanvasGradients {
    val all: List<CanvasGradient> = listOf(
        CanvasGradient("violet", "Vibe", listOf(Color(0xFF7C3AED), Color(0xFFEC4899))),
        CanvasGradient("sunset", "Sunset", listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B))),
        CanvasGradient("ocean", "Ocean", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))),
        CanvasGradient("forest", "Forest", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
        CanvasGradient("midnight", "Midnight", listOf(Color(0xFF0F2027), Color(0xFF2C5364))),
        CanvasGradient("peach", "Peach", listOf(Color(0xFFFFB88C), Color(0xFFDE6262))),
        CanvasGradient("lime", "Lime", listOf(Color(0xFF56AB2F), Color(0xFFA8E063))),
        CanvasGradient("berry", "Berry", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))),
        CanvasGradient("candy", "Candy", listOf(Color(0xFFF857A6), Color(0xFFFF5858))),
        CanvasGradient("sky", "Sky", listOf(Color(0xFF56CCF2), Color(0xFF2F80ED))),
        CanvasGradient("mango", "Mango", listOf(Color(0xFFF7971E), Color(0xFFFFD200))),
        CanvasGradient("rose", "Rose", listOf(Color(0xFFEF629F), Color(0xFFEECDA3))),
        CanvasGradient("aurora", "Aurora", listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
        CanvasGradient("ember", "Ember", listOf(Color(0xFFCB356B), Color(0xFFBD3F32))),
    )

    private val byKey = all.associateBy { it.key }

    val default: CanvasGradient = all.first()

    fun forKey(key: String?): CanvasGradient = key?.let(byKey::get) ?: default

    /** Deterministic gradient for avatars, derived from a stable id. */
    fun forSeed(seed: String): CanvasGradient = all[(seed.hashCode() and Int.MAX_VALUE) % all.size]
}
