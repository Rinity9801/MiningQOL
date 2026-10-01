package forfun.miningqol.client.gui

import forfun.miningqol.client.AutoClickerHUD
import forfun.miningqol.client.AutoClickerManager
import forfun.miningqol.client.AutoForgeManager
import forfun.miningqol.client.CommClaimManager
import forfun.miningqol.client.ExcavatorMacro
import forfun.miningqol.client.AutoFusionManager
import forfun.miningqol.client.EmptyStashManager
import forfun.miningqol.client.EtherwarpClickManager
import forfun.miningqol.client.InShaftClickManager
import forfun.miningqol.client.ShaftClickerManager
import forfun.miningqol.client.ShaftJoinCdManager

/**
 * Cheat-only menu categories (Clickers, Automation) and extra rows, contributed to the dropdown
 * menu. Registered from CheatBootstrap so legit builds never see them.
 */
object CheatGui {
    @JvmStatic
    fun register() {
        registerShatterModules()

        ExtraMiscRows.addToggle("Shaft Join Cooldown", "Block clicks in Glacite GUIs right after you enter a shaft",
            { ShaftJoinCdManager.isEnabled() }) {
            ShaftJoinCdManager.setEnabled(it)
        }
        ExtraWaypointRows.addToggle("Etherwarp Click",
            "Sneak + aim at an etherwarp waypoint (/mqo ether <n>) to right-click",
            { EtherwarpClickManager.isEnabled() }) {
            EtherwarpClickManager.setEnabled(it)
        }
    }

    /**
     * Rows per recorded Auto Forge craft: its name, whether it shows on the picker, and remove.
     * Cached per craft so a field being typed into keeps its identity between frames.
     */
    private val craftRows = java.util.IdentityHashMap<AutoForgeManager.RecordedCraft,
        List<forfun.miningqol.client.shatter.Setting<*>>>()

    private fun recordedCraftSettings(): List<forfun.miningqol.client.shatter.Setting<*>> {
        val crafts = AutoForgeManager.getRecordedCrafts()
        craftRows.keys.retainAll(crafts.toSet())
        return crafts.flatMap { craft ->
            craftRows.getOrPut(craft) {
                listOf(
                    forfun.miningqol.client.shatter.StringSetting("Recorded craft name",
                        "${craft.steps.size} clicks: ${craft.summary()}", "", "Shown on the button")
                        .bind<forfun.miningqol.client.shatter.StringSetting>({ craft.label ?: "" },
                            { v -> craft.label = v; AutoForgeManager.refreshRecordedCrafts() }),
                    forfun.miningqol.client.shatter.BoolSetting("Show on picker", "Hide it without losing the recording", true)
                        .bind<forfun.miningqol.client.shatter.BoolSetting>({ craft.shown },
                            { v -> craft.shown = v; AutoForgeManager.refreshRecordedCrafts() }),
                    forfun.miningqol.client.shatter.ActionSetting("Remove recorded craft", "", true,
                        Runnable {
                            forfun.miningqol.client.shatter.SettingWidgets.stopEditing()
                            AutoForgeManager.removeRecordedCraft(craft)
                        })
                )
            }
        }
    }

    /** The cheat features, declared as dropdown modules. */
    private fun registerShatterModules() {
        forfun.miningqol.client.shatter.ShatterModules.EXTRA_REGISTRARS.add(Runnable {
            val clickers = forfun.miningqol.client.shatter.Category.of("Clickers")
            val automation = forfun.miningqol.client.shatter.Category.of("Automation")
            fun module(
                name: String, description: String, cat: forfun.miningqol.client.shatter.Category,
                get: (() -> Boolean)?, set: ((Boolean) -> Unit)?
            ) = forfun.miningqol.client.shatter.Modules.register(
                forfun.miningqol.client.shatter.Module(name, description, cat,
                    if (get == null) null else java.util.function.BooleanSupplier { get() },
                    if (set == null) null else java.util.function.Consumer { v -> set(v) })
            )
            fun bool(name: String, desc: String, get: () -> Boolean, set: (Boolean) -> Unit) =
                forfun.miningqol.client.shatter.BoolSetting(name, desc, false)
                    .bind<forfun.miningqol.client.shatter.BoolSetting>({ get() }, { v -> set(v) })
            fun slider(name: String, desc: String, min: Double, max: Double, step: Double, unit: String,
                       get: () -> Double, set: (Double) -> Unit) =
                forfun.miningqol.client.shatter.SliderSetting(name, desc, get(), min, max, step, unit)
                    .bind<forfun.miningqol.client.shatter.SliderSetting>({ get() }, { v -> set(v) })

            module("CoalClick", "Auto clicker — toggle with its keybind", clickers,
                { AutoClickerManager.isEnabled() }, { AutoClickerManager.setEnabled(it) })
                .add(slider("Mining Slot", "Hotbar slot of the drill", 1.0, 9.0, 1.0, "",
                    { (AutoClickerManager.getMiningSlot() + 1).toDouble() }, { AutoClickerManager.setMiningSlot(it.toInt() - 1) }))
                .add(bool("Second Drill", "Rotate a second drill into the cycle",
                    { AutoClickerManager.isSecondDrillEnabled() }, { AutoClickerManager.setEnableSecondDrill(it) }))
                .add(slider("Second Drill Slot", "", 1.0, 9.0, 1.0, "",
                    { (AutoClickerManager.getSecondDrillSlot() + 1).toDouble() }, { AutoClickerManager.setSecondDrillSlot(it.toInt() - 1) }))
                .add(slider("Main Drill Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { AutoClickerManager.getMainDrillDelay().toDouble() }, { AutoClickerManager.setMainDrillDelay(it.toInt()) }))
                .add(slider("Second Drill Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { AutoClickerManager.getSecondDrillDelay().toDouble() }, { AutoClickerManager.setSecondDrillDelay(it.toInt()) }))
                .add(bool("HUD", "Show the CoalClick status HUD",
                    { AutoClickerHUD.isEnabled() }, { AutoClickerHUD.setEnabled(it) }))

            module("In Shaft Click", "Cold-aware clicker", clickers,
                { InShaftClickManager.isEnabled() }, { InShaftClickManager.setEnabled(it) })
                .add(slider("Mining Slot", "", 1.0, 9.0, 1.0, "",
                    { (InShaftClickManager.getMiningSlot() + 1).toDouble() }, { InShaftClickManager.setMiningSlot(it.toInt() - 1) }))
                .add(slider("Second Drill Slot", "", 1.0, 9.0, 1.0, "",
                    { (InShaftClickManager.getSecondDrillSlot() + 1).toDouble() }, { InShaftClickManager.setSecondDrillSlot(it.toInt() - 1) }))
                .add(bool("Third Drill", "Swap to a third drill for the ability right-click",
                    { InShaftClickManager.isThirdDrillEnabled() }, { InShaftClickManager.setThirdDrillEnabled(it) }))
                .add(slider("Third Drill Slot", "", 1.0, 9.0, 1.0, "",
                    { (InShaftClickManager.getThirdDrillSlot() + 1).toDouble() }, { InShaftClickManager.setThirdDrillSlot(it.toInt() - 1) }))
                .add(slider("Main Drill Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { InShaftClickManager.getMainDrillDelay().toDouble() }, { InShaftClickManager.setMainDrillDelay(it.toInt()) }))
                .add(slider("Third Drill Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { InShaftClickManager.getSecondDrillDelay().toDouble() }, { InShaftClickManager.setSecondDrillDelay(it.toInt()) }))
                .add(slider("Cold Threshold", "Leave the shaft at this Cold", 0.0, 100.0, 1.0, " cold",
                    { InShaftClickManager.getColdThreshold().toDouble() }, { InShaftClickManager.setColdThreshold(it.toInt()) }))
                .add(bool("Toggle Message", "Chat message when toggled via keybind",
                    { InShaftClickManager.isShowToggleMessage() }, { InShaftClickManager.setShowToggleMessage(it) }))

            module("Shaft Clicker", "Mineshaft clicker", clickers,
                { ShaftClickerManager.isEnabled() }, { ShaftClickerManager.setEnabled(it) })
                .add(slider("Mining Slot", "", 1.0, 9.0, 1.0, "",
                    { (ShaftClickerManager.getMiningSlot() + 1).toDouble() }, { ShaftClickerManager.setMiningSlot(it.toInt() - 1) }))
                .add(bool("Toggle Message", "Chat message when toggled via keybind",
                    { ShaftClickerManager.isShowToggleMessage() }, { ShaftClickerManager.setShowToggleMessage(it) }))

            module("Auto Fusion", "Repeat Previous Fusion + confirm, on a loop", automation,
                { AutoFusionManager.isEnabled() }, { AutoFusionManager.setEnabled(it) })
                .add(slider("Click Delay", "Ticks between clicks", 1.0, 20.0, 1.0, " ticks",
                    { AutoFusionManager.getClickDelay().toDouble() }, { AutoFusionManager.setClickDelay(it.toInt()) }))

            module("Comm Claim", "/claimcomms — auto commission claiming", automation, null, null)
                .add(bool("Auto Trigger", "Run when all mining commissions complete",
                    { CommClaimManager.isAutoTrigger() }, { CommClaimManager.setAutoTrigger(it) }))
                .add(forfun.miningqol.client.shatter.ModeSetting("Claim With",
                    "Abiphone calls Queen Mismyla; Royal Pigeon uses the one in your hotbar",
                    "Abiphone", "Abiphone", "Royal Pigeon")
                    .bind<forfun.miningqol.client.shatter.ModeSetting>(
                        { if (CommClaimManager.isUsePigeon()) "Royal Pigeon" else "Abiphone" },
                        { v -> CommClaimManager.setUsePigeon(v == "Royal Pigeon") }))
                .add(bool("Loadout Swap", "Swap loadouts around the claim via /loadout",
                    { CommClaimManager.isWardrobeSwap() }, { CommClaimManager.setWardrobeSwap(it) }))
                .add(bool("Batch Mining", "Wait for ALL comms before claiming",
                    { CommClaimManager.isBatchMining() }, { CommClaimManager.setBatchMining(it) }))
                .add(bool("Block Input", "Ignore your clicks/keys while a claim runs",
                    { CommClaimManager.isBlockInput() }, { CommClaimManager.setBlockInput(it) }))
                .add(bool("Hide GUI", "Don't render menus during a claim",
                    { CommClaimManager.isHideGui() }, { CommClaimManager.setHideGui(it) }))
                .add(slider("Claim Loadout", "", 1.0, 12.0, 1.0, "",
                    { CommClaimManager.getBatPersonSlot().toDouble() }, { CommClaimManager.setBatPersonSlot(it.toInt()) }))
                .add(slider("Return Loadout", "", 1.0, 12.0, 1.0, "",
                    { CommClaimManager.getDivanSlot().toDouble() }, { CommClaimManager.setDivanSlot(it.toInt()) }))
                .add(slider("Mining Tool Slot", "", 1.0, 9.0, 1.0, "",
                    { (CommClaimManager.getRefinedToolSlot() + 1).toDouble() }, { CommClaimManager.setRefinedToolSlot(it.toInt() - 1) }))
                .add(slider("Pigeon Hold", "How long the pigeon is held before it's right-clicked", 0.0, 1000.0, 5.0, " ms",
                    { CommClaimManager.getPigeonHoldMs().toDouble() }, { CommClaimManager.setPigeonHoldMs(it.toInt()) }))
                .add(slider("Pigeon Release", "How long it stays in hand after the click before switching back", 0.0, 500.0, 5.0, " ms",
                    { CommClaimManager.getPigeonReleaseMs().toDouble() }, { CommClaimManager.setPigeonReleaseMs(it.toInt()) }))
                .add(slider("Action Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { CommClaimManager.getTickDelay().toDouble() }, { CommClaimManager.setTickDelay(it.toInt()) }))
                .add(slider("Menu Timeout", "How long a menu gets to open before the claim gives up", 5.0, 20.0, 1.0, "s",
                    { CommClaimManager.getGuiWaitDelay().coerceAtLeast(5).toDouble() }, { CommClaimManager.setGuiWaitDelay(it.toInt()) }))

            module("Empty Stash", "Supercraft-loop your material stash", automation,
                { EmptyStashManager.isRunning() }, { if (EmptyStashManager.isRunning() != it) EmptyStashManager.toggle() })
                .add(forfun.miningqol.client.shatter.ModeSetting("Material", "What to pull out of the stash",
                    EmptyStashManager.getMaterial().displayName,
                    EmptyStashManager.Material.entries.map { it.displayName })
                    .bind<forfun.miningqol.client.shatter.ModeSetting>(
                        { EmptyStashManager.getMaterial().displayName },
                        { name -> EmptyStashManager.setMaterial(
                            EmptyStashManager.Material.entries.firstOrNull { it.displayName == name }) }))
                .add(slider("Action Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { EmptyStashManager.getActionDelay().toDouble() }, { EmptyStashManager.setActionDelay(it.toInt()) }))
                .add(forfun.miningqol.client.shatter.ActionSetting("Run", "Stops on its own when the stash is empty", false,
                    Runnable { EmptyStashManager.toggle() })
                    .label { if (EmptyStashManager.isRunning()) "Stop" else "Start" })

            module("Excavator", "/excavator — Fossil Excavator scrap digger", automation, null, null)
                .add(bool("No Delay", "Click as soon as the grid updates instead of waiting the tick delay",
                    { ExcavatorMacro.isNoDelay() }, { ExcavatorMacro.setNoDelay(it) }))
                .add(slider("Tick Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { ExcavatorMacro.getTickDelay().toDouble() }, { ExcavatorMacro.setTickDelay(it.toInt()) }))
                .add(forfun.miningqol.client.shatter.ActionSetting("Run",
                    "Stand next to the Fossil Excavator first; stops when you walk away or run out of scrap", false,
                    Runnable { ExcavatorMacro.toggle() })
                    .label { if (ExcavatorMacro.isRunning()) "Stop" else "Start" })

            val autoForge = module("Auto Forge", "Craft picker whenever The Forge opens", automation,
                { AutoForgeManager.isEnabled() }, { AutoForgeManager.setEnabled(it) })
                .add(slider("Tick Delay", "", 1.0, 10.0, 1.0, " ticks",
                    { AutoForgeManager.getTickDelay().toDouble() }, { AutoForgeManager.setTickDelay(it.toInt()) }))
                .add(slider("Amount", "Runs per click", 1.0, 7.0, 1.0, "x",
                    { AutoForgeManager.getRunCount().toDouble() }, { AutoForgeManager.setRunCount(it.toInt()) }))
            for (label in AutoForgeManager.builtinLabels()) {
                autoForge.add(bool(label, "Show this craft on the picker",
                    { AutoForgeManager.isBuiltinShown(label) }, { AutoForgeManager.setBuiltinShown(label, it) }))
            }
            autoForge.add(forfun.miningqol.client.shatter.ActionSetting("Record",
                "Press, open The Forge and click through a craft once; it saves when The Forge reopens", false,
                Runnable { AutoForgeManager.armRecording() })
                .label { if (AutoForgeManager.isRecordArmed()) "Armed — open The Forge (click to cancel)" else "Record next craft" })
            autoForge.dynamicSettings { recordedCraftSettings() }

            forfun.miningqol.client.shatter.Modules.register(
                forfun.miningqol.client.shatter.Module("HOTM Presets",
                    "Heart of the Mountain editor + auto-apply", automation,
                    Runnable {
                        net.minecraft.client.Minecraft.getInstance().schedule {
                            forfun.miningqol.client.hotm.HotmChestScreen.open()
                        }
                    }))
        })
    }

}
