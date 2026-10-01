package forfun.miningqol.client.gui

/**
 * Palette for the remaining Vexel screen (the mineshaft sign-up editor): near-black floating
 * panels with a faint hairline edge, and the accent colours it tints its buttons with.
 */
object SettingsUi {
    const val PANEL_BG = 0xFF0D0D0F.toInt()
    const val CARD_BG = 0xFF161618.toInt()
    const val CARD_BORDER = 0xFF252528.toInt()
    const val CARD_HOVER = 0xFF1E1E20.toInt()
    const val NAV_SELECTED = 0xFF1E1E20.toInt()
    const val TRACK = 0xFF1A1A1A.toInt()
    const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    const val TEXT_SECONDARY = 0xFFBBBBBB.toInt()
    const val TEXT_MUTED = 0xFF888888.toInt()
    const val TEXT_DIM = 0xFF606060.toInt()
    const val EDGE_WIDTH = 0.5f
    /** The edge round each floating panel: a faint light hairline, subtle but always there. */
    const val PANEL_OUTLINE = 0x29FFFFFF

    const val GREEN = 0xFF9ECE6A.toInt()
    const val CYAN = 0xFF7DCFFF.toInt()
    const val PURPLE = 0xFFBB9AF7.toInt()
    const val SKY = 0xFF89DDFF.toInt()
    const val YELLOW = 0xFFE0AF68.toInt()
    const val RED = 0xFFF7768E.toInt()

    /** Surface opacity: panels and cards are drawn this solid. */
    private const val OPACITY = 0.9f

    /** Scales a color's alpha channel by the surface opacity. */
    fun alpha(color: Int): Int {
        val a = ((color ushr 24) and 0xFF) * OPACITY
        return (a.toInt().coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)
    }

    /** The accent color with its alpha replaced (0f..1f) — for tinted chips and pills. */
    fun tint(accent: Int, alphaFrac: Float): Int =
        ((alphaFrac * 255f).toInt().coerceIn(0, 255) shl 24) or (accent and 0xFFFFFF)

    /** Softer color for thin control outlines. */
    fun edge(color: Int, strength: Float = 0.72f): Int {
        val sourceAlpha = (color ushr 24) and 0xFF
        val a = (sourceAlpha * OPACITY * strength).toInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0xFFFFFF)
    }
}
