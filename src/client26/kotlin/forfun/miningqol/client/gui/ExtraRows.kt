package forfun.miningqol.client.gui

/**
 * Extra rows for the Misc module, contributed at runtime by the cheat
 * source tree.
 */
object ExtraMiscRows {
    class Toggle(
        @JvmField val title: String,
        @JvmField val description: String,
        @JvmField val get: () -> Boolean,
        @JvmField val set: (Boolean) -> Unit
    )

    @JvmField
    val toggles = mutableListOf<Toggle>()

    @JvmStatic
    fun addToggle(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        toggles.add(Toggle(title, description, get, set))
    }
}

/**
 * Extra rows for the Ordered Waypoints feature, contributed at runtime by the
 * cheat source tree — e.g. the etherwarp auto-click toggle.
 */
object ExtraWaypointRows {
    @JvmField
    val toggles = mutableListOf<ExtraMiscRows.Toggle>()

    @JvmStatic
    fun addToggle(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        toggles.add(ExtraMiscRows.Toggle(title, description, get, set))
    }
}

/**
 * Extra rows for the Corpse/Shaft ESP features, contributed at runtime — used by the
 * local-only ESP feed module so the released screens never reference it.
 */
object ExtraEspRows {
    @JvmField
    val corpse = mutableListOf<ExtraMiscRows.Toggle>()

    @JvmField
    val shaft = mutableListOf<ExtraMiscRows.Toggle>()

    @JvmStatic
    fun addCorpse(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        corpse.add(ExtraMiscRows.Toggle(title, description, get, set))
    }

    @JvmStatic
    fun addShaft(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        shaft.add(ExtraMiscRows.Toggle(title, description, get, set))
    }
}
