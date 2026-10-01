package forfun.miningqol.client.shatter;

import forfun.miningqol.client.BlockOverlay;
import forfun.miningqol.client.CommTracker;
import forfun.miningqol.client.CommissionGui;
import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.CritParticleDrop;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.EntityEspMode;
import forfun.miningqol.client.FiletWarning;
import forfun.miningqol.client.ForgeDisplay;
import forfun.miningqol.client.MayhemHUD;
import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.MqoChat;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.RollingMinerCooldown;
import forfun.miningqol.client.ShaftESP;
import forfun.miningqol.client.SkinMob;
import forfun.miningqol.client.SoundBlocker;
import forfun.miningqol.client.WispRadius;
import forfun.miningqol.client.gui.ExtraEspRows;
import forfun.miningqol.client.gui.ExtraMiscRows;
import forfun.miningqol.client.gui.ExtraWaypointRows;
import forfun.miningqol.client.gui.HudPositionScreen;
import forfun.miningqol.client.gui.MineshaftAutoPartyScreen;
import forfun.miningqol.client.party.MineshaftAutoParty;
import forfun.miningqol.client.party.PartyAutoAccept;
import forfun.miningqol.client.summary.ShaftPbType;
import forfun.miningqol.client.summary.ShaftSummary;
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * The skinned menu's view of the mod: every feature declared as a {@link Module} whose toggle and
 * settings delegate straight into the same live state the classic Vexel GUI edits. Built once, on
 * first open; the cheat tree contributes its categories through {@link #EXTRA_REGISTRARS}.
 */
public final class ShatterModules {
    private ShatterModules() {}

    /** Cheat/local-only registrars, added before the menu is first opened. */
    public static final List<Runnable> EXTRA_REGISTRARS = new ArrayList<>();

    private static boolean registered;

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        registerBuiltins();
        for (Runnable r : EXTRA_REGISTRARS) r.run();
    }

    private static void registerBuiltins() {
        Category general = Category.of("General");
        Category huds = Category.of("HUDs");
        Category waypoints = Category.of("Waypoints");
        Category esp = Category.of("ESP");

        // ---- General ----

        Modules.register(new Module("Click GUI", "How this menu looks and behaves", general, null, null)
            .add(new SliderSetting("Scale", "Size of the menu, relative to a 1080p screen", 1.0, 0.75, 2.0, 0.05, "×")
                .bind(() -> (double) ShatterUi.scale, v -> ShatterUi.setScale(v.floatValue())))
            .add(new BoolSetting("Animations", "Ease panels, switches and blooms", true)
                .bind(ShatterUi::animations, ShatterUi::setAnimations))
            .add(new BoolSetting("Descriptions", "Show a feature's description when hovering it", true)
                .bind(ShatterUi::descriptions, ShatterUi::setDescriptions))
            .add(ShatterUi.opacity)
            .add(ShatterUi.outline)
            .add(ShatterUi.outlineWidth));

        Modules.register(new Module("Misc", "Chat, sounds, and small overlays", general, null, null)
            .add(new BoolSetting("Mod Chat Messages", "Status chatter in chat; command output never hidden", true)
                .bind(MqoChat::isLogsEnabled, MqoChat::setLogsEnabled))
            .add(new BoolSetting("Crit Particle Drop", "Sneaking lowers crit particles to the registered spot", false)
                .bind(CritParticleDrop::isEnabled, CritParticleDrop::setEnabled))
            .add(new BoolSetting("Remove Corpse Ding", "Mutes Hypixel's corpse note-block ding", false)
                .bind(SoundBlocker::isCorpseDingBlocked, SoundBlocker::setCorpseDingBlocked))
            .add(new BoolSetting("Auto-skip /sho load", "Runs /sho skipto 1 after /sho load", false)
                .bind(() -> MiningqolClient.getConfig() != null && MiningqolClient.getConfig().autoSkipShoLoad,
                    v -> { if (MiningqolClient.getConfig() != null) MiningqolClient.getConfig().autoSkipShoLoad = v; }))
            .add(new BoolSetting("Filet O' Fortune Warning", "Warn when your Filet O' Fortune cake expires", false)
                .bind(FiletWarning::isEnabled, FiletWarning::setEnabled))
            .add(new BoolSetting("Efficient Miner Overlay", "Heatmap the best clay / red sandstone (Glacite)", false)
                .bind(EfficientMinerOverlay::isEnabled, EfficientMinerOverlay::setEnabled))
            .add(new BoolSetting("Old Heatmap Colors", "Use the legacy 8-colour heatmap palette", false)
                .bind(EfficientMinerOverlay::isUsingOldHeatmap, EfficientMinerOverlay::setUseOldHeatmap)));

        Modules.register(new Module("Block Overlay", "Custom targeted block highlight", general,
            BlockOverlay::isEnabled, BlockOverlay::setEnabled)
            .add(new ModeSetting("Mode", "Overlay style", BlockOverlay.getMode().getDisplayName(), overlayModeNames())
                .bind(() -> BlockOverlay.getMode().getDisplayName(), ShatterModules::setOverlayMode))
            .add(ColorSetting.ofRgb("Outline Color", "Edge colour of the overlay",
                BlockOverlay::getOutlineColor, BlockOverlay::setOutlineColor,
                BlockOverlay::getOutlineAlpha, BlockOverlay::setOutlineAlpha))
            .add(ColorSetting.ofRgb("Fill Color", "Fill colour of the overlay",
                BlockOverlay::getFillColor, BlockOverlay::setFillColor,
                BlockOverlay::getFillAlpha, BlockOverlay::setFillAlpha))
            .add(new SliderSetting("Line Width", "Outline thickness", 3.0, 1.0, 10.0, 0.1, " px")
                .bind(() -> (double) BlockOverlay.getLineWidth(), v -> BlockOverlay.setLineWidth(v.floatValue())))
            .add(new BoolSetting("Phase", "Draw the overlay through walls", false)
                .bind(BlockOverlay::isPhase, BlockOverlay::setPhase))
            .add(new BoolSetting("Hide with Etherwarp", "Hide while sneaking with an ethermerged AOTE/AOTV", false)
                .bind(BlockOverlay::isHideDuringEtherwarp, BlockOverlay::setHideDuringEtherwarp)));

        Modules.register(new Module("Mineshaft Auto Party", "Warp players into the shafts they want", general,
            MineshaftAutoParty::isEnabled, MineshaftAutoParty::setEnabled)
            .add(new BoolSetting("Disband After Warp", "Disband 2s after warping", false)
                .bind(MineshaftAutoParty::isDisbandAfterWarp, MineshaftAutoParty::setDisbandAfterWarp))
            .add(new SliderSetting("Settle Delay", "How long to wait for the tab list",
                5, MineshaftAutoParty.MIN_SETTLE_SECONDS, MineshaftAutoParty.MAX_SETTLE_SECONDS, 1, "s")
                .bind(() -> (double) MineshaftAutoParty.getSettleSeconds(), v -> MineshaftAutoParty.setSettleSeconds(v.intValue())))
            .add(new BoolSetting("Disband On Timeout", "Send /p disband when nobody joins in time", false)
                .bind(MineshaftAutoParty::isDisbandOnTimeout, MineshaftAutoParty::setDisbandOnTimeout))
            .add(new SliderSetting("Disband Timeout", "Seconds before the timeout disband",
                10, MineshaftAutoParty.MIN_DISBAND_SECONDS, MineshaftAutoParty.MAX_DISBAND_SECONDS, 1, "s")
                .bind(() -> (double) MineshaftAutoParty.getDisbandSeconds(), v -> MineshaftAutoParty.setDisbandSeconds(v.intValue())))
            .add(new SliderSetting("Warp Delay", "Seconds after the last join before warping",
                2, MineshaftAutoParty.MIN_WARP_DELAY_SECONDS, MineshaftAutoParty.MAX_WARP_DELAY_SECONDS, 0.5, "s")
                .bind(() -> (double) MineshaftAutoParty.getWarpDelaySeconds(), v -> MineshaftAutoParty.setWarpDelaySeconds(v.floatValue())))
            .add(new BoolSetting("Auto Accept Invites", "Accept party invites from the auto-accept list", false)
                .bind(PartyAutoAccept::isEnabled, PartyAutoAccept::setEnabled))
            .add(new BoolSetting("Not During Ability", "Ignore invites while a pickaxe ability runs", false)
                .bind(PartyAutoAccept::isBlockDuringAbility, PartyAutoAccept::setBlockDuringAbility))
            .add(new BoolSetting("Not When Ability Ready", "Ignore invites when the ability is off cooldown", false)
                .bind(PartyAutoAccept::isBlockWhenReady, PartyAutoAccept::setBlockWhenReady))
            .add(new BoolSetting("Not While In A Shaft", "Ignore invites until you leave your shaft", false)
                .bind(PartyAutoAccept::isBlockInShaft, PartyAutoAccept::setBlockInShaft))
            .add(new ActionSetting("Edit Sign-ups", "Open the sign-up editor", false,
                () -> openScreen(MineshaftAutoPartyScreen::new))));

        Modules.register(new Module("Shaft Sign-ups", "Open the sign-up editor", general,
            () -> openScreen(MineshaftAutoPartyScreen::new)));

        // The mod's vanilla KeyMappings, rebindable in place: click the key box, press a key
        // (right-click clears). Same mappings as Controls -> MiningQOL; saves to options.txt.
        Module keybinds = new Module("Keybinds", "Rebind the mod's hotkeys", general, null, null);
        for (net.minecraft.client.KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (!mapping.getName().startsWith("key.miningqol.")) continue;
            keybinds.add(new KeySetting(
                net.minecraft.client.resources.language.I18n.get(mapping.getName()), "Toggle key", KeySetting.NONE)
                .bind(
                    () -> {
                        com.mojang.blaze3d.platform.InputConstants.Key bound =
                            net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mapping);
                        return bound.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                            ? bound.getValue() : KeySetting.NONE;
                    },
                    code -> {
                        mapping.setKey(code == KeySetting.NONE
                            ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN
                            : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(code));
                        net.minecraft.client.KeyMapping.resetMapping();
                        Minecraft.getInstance().options.save();
                    }));
        }
        Modules.register(keybinds);

        Modules.register(new Module("Command Keybinds", "Bind commands to keys", general, null, null)
            .dynamicSettings(CommandKeybindRows::settings));

        // ---- HUDs ----

        Modules.register(new Module("Move HUDs", "Open the HUD position editor", huds,
            () -> openScreen(HudPositionScreen::new)));

        Modules.register(new Module("Commission HUD", "On-screen commission tracker", huds,
            CommissionHUD::isEnabled, CommissionHUD::setEnabled)
            .add(new BoolSetting("Hide with F1", "Hide when the vanilla HUD is hidden", false)
                .bind(CommissionHUD::isHideWithF1, CommissionHUD::setHideWithF1))
            .add(new ModeSetting("Display Style", "Panel is the card look; the rest are plain text",
                "Panel", styleNames())
                .bind(() -> CommissionHUD.getDisplayStyle().displayName, ShatterModules::setCommissionStyle))
            .add(new SliderSetting("Scale", "HUD scale", 1.0, 0.5, 2.0, 0.05, "×")
                .bind(() -> (double) CommissionHUD.getScale(), v -> CommissionHUD.setScale(v.floatValue())))
            .add(new BoolSetting("Background", "Dark backdrop behind the panel", true)
                .bind(CommissionHUD::isBackgroundEnabled, CommissionHUD::setBackgroundEnabled))
            .add(new ModeSetting("Layout", "Panel arrangement", "2x2 Grid", "2x2 Grid", "Stacked Column")
                .bind(() -> CommissionHUD.getLayoutMode() == CommissionHUD.LayoutMode.COLUMN ? "Stacked Column" : "2x2 Grid",
                    v -> CommissionHUD.setLayoutMode("Stacked Column".equals(v)
                        ? CommissionHUD.LayoutMode.COLUMN : CommissionHUD.LayoutMode.GRID)))
            .add(new BoolSetting("Show Over Menus", "Keep the panel on top of Hypixel menus", true)
                .bind(CommissionHUD::isShowOverMenus, CommissionHUD::setShowOverMenus))
            .add(new BoolSetting("Commission Stats", "Total completed + comms/hour", false)
                .bind(CommTracker::isStatsEnabled, CommTracker::setStatsEnabled))
            .add(new BoolSetting("Show Header", "Lead the text styles with \"Commissions:\"", true)
                .bind(CommissionHUD::isShowHeader, CommissionHUD::setShowHeader))
            .add(new BoolSetting("Progress Colors", "Percent coloured by progress tier", false)
                .bind(CommissionHUD::isProgressColors, CommissionHUD::setProgressColors))
            .add(ColorSetting.ofRgb("Header Color", "Text styles only", CommissionHUD::getHeaderColor, CommissionHUD::setHeaderColor))
            .add(ColorSetting.ofRgb("Name Color", "Text styles only", CommissionHUD::getNameColor, CommissionHUD::setNameColor))
            .add(ColorSetting.ofRgb("Percent Color", "Text styles only", CommissionHUD::getPercentColor, CommissionHUD::setPercentColor))
            .add(ColorSetting.ofRgb("Done Color", "Text styles only", CommissionHUD::getDoneColor, CommissionHUD::setDoneColor)));

        Modules.register(new Module("Comm Menu GUI", "Odin-style commissions menu replacement", huds,
            CommissionGui::isEnabled, CommissionGui::setEnabled)
            .add(new SliderSetting("Scale", "Menu scale", 1.0, 0.5, 2.0, 0.05, "×")
                .bind(() -> (double) CommissionGui.getScale(), v -> CommissionGui.setScale(v.floatValue())))
            .add(new SliderSetting("Button Size", "Size of the commission squares", 24, 12, 48, 1, " px")
                .bind(() -> (double) CommissionGui.getButtonSize(), v -> CommissionGui.setButtonSize(v.floatValue())))
            .add(new BoolSetting("Show Progress", "Percent on commission squares", true)
                .bind(CommissionGui::isShowProgress, CommissionGui::setShowProgress))
            .add(ColorSetting.ofRgb("Accent Color", "Colour of in-progress commission squares",
                CommissionGui::getAccentColor, CommissionGui::setAccentColor))
            .add(ColorSetting.ofRgb("Completed Color", "Colour of a claimable commission's square",
                CommissionGui::getDoneColor, CommissionGui::setDoneColor)));

        Modules.register(new Module("Pickaxe Cooldown", "Ability cooldown HUD + ready alert", huds,
            PickaxeCooldownHUD::isEnabled, PickaxeCooldownHUD::setEnabled)
            .add(new BoolSetting("Hide with F1", "Hide when the vanilla HUD is hidden", false)
                .bind(PickaxeCooldownHUD::isHideWithF1, PickaxeCooldownHUD::setHideWithF1))
            .add(new ModeSetting("Text Align", "Where the line sits in the HUD box", "Center", "Left", "Center", "Right")
                .bind(() -> List.of("Left", "Center", "Right").get(PickaxeCooldownHUD.getTextAlign()),
                    v -> PickaxeCooldownHUD.setTextAlign(List.of("Left", "Center", "Right").indexOf(v))))
            .add(new BoolSetting("Seconds Suffix", "Off shows a bare number instead of 30s", true)
                .bind(PickaxeCooldownHUD::isSecondsSuffix, PickaxeCooldownHUD::setSecondsSuffix))
            .add(new BoolSetting("Cooldown Only", "Hide the ability name", false)
                .bind(PickaxeCooldownHUD::isCooldownOnly, PickaxeCooldownHUD::setCooldownOnly))
            .add(new BoolSetting("Custom Cooldown", "Local timer instead of reading the tab list", false)
                .bind(PickaxeCooldownHUD::isCustomCooldownEnabled, PickaxeCooldownHUD::setCustomCooldownEnabled))
            .add(new SliderSetting("Custom Seconds", "Used when Custom Cooldown is on", 120, 1, 600, 1, "s")
                .bind(() -> (double) PickaxeCooldownHUD.getCustomCooldownSeconds(),
                    v -> PickaxeCooldownHUD.setCustomCooldownSeconds(v.intValue())))
            .add(new SliderSetting("Scale", "HUD scale", 1.0, 0.5, 2.0, 0.05, "×")
                .bind(() -> (double) PickaxeCooldownHUD.getScale(), v -> PickaxeCooldownHUD.setScale(v.floatValue())))
            .add(new BoolSetting("Ability Active Timer", "Count down the ability's active window", true)
                .bind(PickaxeCooldownHUD::isActiveTimerEnabled, PickaxeCooldownHUD::setActiveTimerEnabled))
            .add(new BoolSetting("Ready Title", "Flash a title when it comes off cooldown", true)
                .bind(PickaxeCooldownHUD::isTitleEnabled, PickaxeCooldownHUD::setTitleEnabled))
            .add(new SliderSetting("Title Threshold", "Seconds left when the title starts", 5, 0, 30, 1, "s")
                .bind(() -> (double) PickaxeCooldownHUD.getTitleThreshold(),
                    v -> PickaxeCooldownHUD.setTitleThreshold(v.intValue())))
            .add(ColorSetting.ofRgb("Cooldown Label", "", PickaxeCooldownHUD::getCooldownLabelColor, PickaxeCooldownHUD::setCooldownLabelColor))
            .add(ColorSetting.ofRgb("Cooldown Value", "", PickaxeCooldownHUD::getCooldownValueColor, PickaxeCooldownHUD::setCooldownValueColor))
            .add(ColorSetting.ofRgb("Ready Label", "", PickaxeCooldownHUD::getReadyLabelColor, PickaxeCooldownHUD::setReadyLabelColor))
            .add(ColorSetting.ofRgb("Ready Value", "", PickaxeCooldownHUD::getReadyValueColor, PickaxeCooldownHUD::setReadyValueColor))
            .add(ColorSetting.ofRgb("Active Label", "", PickaxeCooldownHUD::getActiveLabelColor, PickaxeCooldownHUD::setActiveLabelColor))
            .add(ColorSetting.ofRgb("Active Value", "", PickaxeCooldownHUD::getActiveValueColor, PickaxeCooldownHUD::setActiveValueColor)));

        Modules.register(new Module("Forge Display", "Forge slots and times from the tab list", huds,
            ForgeDisplay::isEnabled, ForgeDisplay::setEnabled)
            .add(new BoolSetting("Hide with F1", "Hide when the vanilla HUD is hidden", false)
                .bind(ForgeDisplay::isHideWithF1, ForgeDisplay::setHideWithF1))
            .add(new BoolSetting("Sort By Time Left", "Soonest first", true)
                .bind(ForgeDisplay::isSortByTime, ForgeDisplay::setSortByTime))
            .add(new BoolSetting("Show Empty Slots", "List slots with nothing forging", false)
                .bind(ForgeDisplay::isShowEmpty, ForgeDisplay::setShowEmpty))
            .add(ColorSetting.ofRgb("Header Color", "", ForgeDisplay::getTitleColor, ForgeDisplay::setTitleColor))
            .add(ColorSetting.ofRgb("Item Color", "", ForgeDisplay::getItemColor, ForgeDisplay::setItemColor))
            .add(ColorSetting.ofRgb("Time Color", "", ForgeDisplay::getTimeColor, ForgeDisplay::setTimeColor))
            .add(ColorSetting.ofRgb("Ready Color", "", ForgeDisplay::getReadyColor, ForgeDisplay::setReadyColor)));

        Modules.register(new Module("Mayhem HUD", "Which Mineshaft Mayhem buff you rolled", huds,
            MayhemHUD::isEnabled, MayhemHUD::setEnabled)
            .add(new BoolSetting("Hide with F1", "Hide when the vanilla HUD is hidden", false)
                .bind(MayhemHUD::isHideWithF1, MayhemHUD::setHideWithF1))
            .add(new BoolSetting("Always Show", "Draw \"Mayhem: None\" outside shafts", false)
                .bind(MayhemHUD::isAlwaysShow, MayhemHUD::setAlwaysShow))
            .add(ColorSetting.ofRgb("Label Color", "", MayhemHUD::getLabelColor, MayhemHUD::setLabelColor))
            .add(ColorSetting.ofRgb("Buff Color", "", MayhemHUD::getValueColor, MayhemHUD::setValueColor))
            .add(ColorSetting.ofRgb("None Color", "", MayhemHUD::getNoneColor, MayhemHUD::setNoneColor)));

        Modules.register(new Module("Shaft Summary", "Fines, coins and rank after every gem shaft", general,
            ShaftSummary::isEnabled, ShaftSummary::setEnabled)
            .add(new BoolSetting("Print to Chat", "Show the summary block in chat", true)
                .bind(ShaftSummary::isPrintToChat, ShaftSummary::setPrintToChat))
            .add(new BoolSetting("Print to Party", "Send a one-line recap to /pc", false)
                .bind(ShaftSummary::isPrintToParty, ShaftSummary::setPrintToParty))
            .add(new BoolSetting("Show Buffs in Summary", "Add a Buffs line to the summary", false)
                .bind(ShaftSummary::isShowBuffsInSummary, ShaftSummary::setShowBuffsInSummary))
            .add(new BoolSetting("Front Loaded", "Tag runs as front loaded", false)
                .bind(ShaftSummary::isFrontLoaded, ShaftSummary::setFrontLoaded))
            .add(new BoolSetting("Track All Shaft Types", "Off: only Jasper shafts are tracked", true)
                .bind(ShaftSummary::isTrackAllShaftSummaries, ShaftSummary::setTrackAllShaftSummaries))
            .add(new BoolSetting("Auto Save", "Keep runs for the Rank line", true)
                .bind(ShaftSummary::isAutoSave, ShaftSummary::setAutoSave))
            .add(new SliderSetting("Max Saved Runs", "History size; weakest runs are dropped first", 100, 0, 1000, 10, "")
                .bind(() -> (double) ShaftSummary.getMaxSavedRuns(), v -> ShaftSummary.setMaxSavedRuns((int) Math.round(v))))
            .add(new BoolSetting("Track Efficiency in Summary", "Blocks mined vs a nonstop miner", true)
                .bind(ShaftSummary::isTrackEfficiencyInSummary, ShaftSummary::setTrackEfficiencyInSummary))
            .add(new SliderSetting("Efficiency Reset Delay", "Idle seconds before the max-miner pauses", 15, 1, 120, 1, "s")
                .bind(() -> (double) ShaftSummary.getEfficiencyPauseSeconds(), v -> ShaftSummary.setEfficiencyPauseSeconds((int) Math.round(v))))
            .add(new SliderSetting("Gem Mining Speed", "0 = read from the tab list", 0, 0, 20000, 50, "")
                .bind(() -> (double) ShaftSummary.getGemMiningSpeed(), v -> ShaftSummary.setGemMiningSpeed((int) Math.round(v))))
            .add(new BoolSetting("Blue Cheese", "Mining Speed Boost lasts 25s", false)
                .bind(ShaftSummary::isBlueCheese, ShaftSummary::setBlueCheese))
            .add(new ModeSetting("Coin Price", "Bazaar price for the Money line", "Sell Offer", "Sell Offer", "Instant Sell")
                .bind(() -> ShaftSummary.isSellOffer() ? "Sell Offer" : "Instant Sell", v -> ShaftSummary.setSellOffer(v.equals("Sell Offer"))))
            .add(new ModeSetting("Value As", "Gem quality the coin value is based on", "Flawless", "Fine", "Flawless")
                .bind(() -> ShaftSummary.isValueAsFlawless() ? "Flawless" : "Fine", v -> ShaftSummary.setValueAsFlawless(v.equals("Flawless"))))
            .add(new MultiSetting("Group Ranks By", "Only rank against runs sharing these", ShaftSummary.getRanksBy(),
                    ShaftSummary.RANK_CRITERIA.toArray(new String[0]))
                .bind(ShaftSummary::getRanksBy, ShaftSummary::setRanksBy))
            .add(new MultiSetting("Summarize Which Shafts", "Shaft types that print a summary", ShaftSummary.getSummaryShaftTypes(), shaftTypeKeys())
                .bind(ShaftSummary::getSummaryShaftTypes, ShaftSummary::setSummaryShaftTypes))
            .add(new MultiSetting("Save Which Shafts", "Shaft types kept in the history", ShaftSummary.getSavedShaftTypes(), shaftTypeKeys())
                .bind(ShaftSummary::getSavedShaftTypes, ShaftSummary::setSavedShaftTypes))
            .add(new ActionSetting("Print Preview", "Print a sample summary in chat", false,
                () -> ShaftSummary.printSample(Minecraft.getInstance()))));

        Modules.register(new Module("Mineshaft Portal", "Outline the portal you just found, through walls", esp,
            forfun.miningqol.client.MineshaftPortal::isEnabled, forfun.miningqol.client.MineshaftPortal::setEnabled)
            .add(new BoolSetting("Tracer", "Line from your crosshair to the portal", true)
                .bind(forfun.miningqol.client.MineshaftPortal::isTracer, forfun.miningqol.client.MineshaftPortal::setTracer))
            .add(ColorSetting.ofRgb("Colour", "Outline and tracer colour",
                forfun.miningqol.client.MineshaftPortal::getColor, forfun.miningqol.client.MineshaftPortal::setColor,
                forfun.miningqol.client.MineshaftPortal::getAlpha, forfun.miningqol.client.MineshaftPortal::setAlpha))
            .add(new SliderSetting("Line Width", "Thickness of the outline and tracer", 1.5, 0.5, 5.0, 0.1, "×")
                .bind(() -> (double) forfun.miningqol.client.MineshaftPortal.getLineWidth(),
                    v -> forfun.miningqol.client.MineshaftPortal.setLineWidth(v.floatValue()))));

        Modules.register(new Module("Wisp Radius", "Blocks you can stand on inside a Will-o'-wisp's range", esp,
            WispRadius::isEnabled, WispRadius::setEnabled)
            .add(new BoolSetting("Edge Only", "Only paint the outer two blocks of the radius", false)
                .bind(WispRadius::isEdgeOnly, WispRadius::setEdgeOnly))
            .add(new BoolSetting("Floor Blocks Only", "Only paint snow, snow layers and smooth stone", true)
                .bind(WispRadius::isFloorBlocksOnly, WispRadius::setFloorBlocksOnly))
            .add(new SliderSetting("Radius", "Effect radius in blocks", 30, 1, 64, 1, " blocks")
                .bind(() -> (double) WispRadius.getRadius(), v -> WispRadius.setRadius(v.floatValue())))
            .add(new SliderSetting("Opacity", "Highlight opacity", 0.35, 0.05, 1.0, 0.05, "")
                .bind(() -> (double) WispRadius.getAlpha(), v -> WispRadius.setAlpha(v.floatValue())))
            .add(ColorSetting.ofRgb("Color", "", WispRadius::getColor, WispRadius::setColor)));

        Modules.register(new Module("Rolling Miner", "20-second HUD timer after double drops", huds,
            RollingMinerCooldown::isEnabled, RollingMinerCooldown::setEnabled)
            .add(ColorSetting.ofRgb("Cooldown Label", "", RollingMinerCooldown::getCooldownLabelColor, RollingMinerCooldown::setCooldownLabelColor))
            .add(ColorSetting.ofRgb("Cooldown Value", "", RollingMinerCooldown::getCooldownValueColor, RollingMinerCooldown::setCooldownValueColor))
            .add(ColorSetting.ofRgb("Ready Label", "", RollingMinerCooldown::getReadyLabelColor, RollingMinerCooldown::setReadyLabelColor))
            .add(ColorSetting.ofRgb("Ready Value", "", RollingMinerCooldown::getReadyValueColor, RollingMinerCooldown::setReadyValueColor)));

        itemTimer(huds, forfun.miningqol.client.ItemCooldownTimer.ROGUE_SWORD, "30-second timer after right-clicking a Rogue Sword");
        itemTimer(huds, forfun.miningqol.client.ItemCooldownTimer.TUBA, "20-second timer after right-clicking a Weird or Weirder Tuba");

        // ---- Waypoints ----

        Module ordered = new Module("Ordered Waypoints", "Guided mining routes (/mqo)", waypoints,
            OrderedWaypointManager::isEnabledRaw, OrderedWaypointManager::setEnabled)
            .add(new BoolSetting("Trace Line", "Line from your crosshair to the next waypoint", false)
                .bind(OrderedWaypointManager::isTraceLineEnabled, OrderedWaypointManager::setTraceLineEnabled))
            .add(new BoolSetting("Show Number", "Waypoint #number on the label", true)
                .bind(OrderedWaypointManager::isShowName, OrderedWaypointManager::setShowName))
            .add(new BoolSetting("Show Distance", "Distance in meters on the label", true)
                .bind(OrderedWaypointManager::isShowDistance, OrderedWaypointManager::setShowDistance))
            .add(new SliderSetting("Trigger Range", "How close counts as reached", 3, 1, 10, 0.5, " blocks")
                .bind(() -> (double) OrderedWaypointManager.getWaypointRange(),
                    v -> OrderedWaypointManager.setWaypointRange(v.floatValue())))
            .add(new SliderSetting("Upcoming Waypoints", "How many upcoming waypoints to show", 2, 0, 5, 1, " shown")
                .bind(() -> (double) OrderedWaypointManager.getNextCount(),
                    v -> OrderedWaypointManager.setNextCount(v.intValue())));
        for (ExtraMiscRows.Toggle row : ExtraWaypointRows.toggles) {
            ordered.add(boolOf(row));
        }
        Modules.register(ordered);

        Modules.register(new Module("Block Check", "Lobby check + mined-out skipping", waypoints,
            OrderedWaypointManager::isLobbyCheckEnabled, OrderedWaypointManager::setLobbyCheckEnabled)
            .add(new StringSetting("Lobby Check Block", "Block id to expect", "", "minecraft:coal_ore")
                .bind(OrderedWaypointManager::getLobbyCheckBlock, v -> OrderedWaypointManager.setLobbyCheckBlock(v.trim())))
            .add(new SliderSetting("Check Interval", "Check every N waypoints", 5, 2, 30, 1, "")
                .bind(() -> (double) OrderedWaypointManager.getLobbyCheckInterval(),
                    v -> OrderedWaypointManager.setLobbyCheckInterval(v.intValue())))
            .add(new SliderSetting("Check Radius", "Blocks around each waypoint to scan", 2, 1, 5, 1, " blocks")
                .bind(() -> (double) OrderedWaypointManager.getLobbyCheckRadius(),
                    v -> OrderedWaypointManager.setLobbyCheckRadius(v.intValue())))
            .add(new BoolSetting("Skip Mined-Out Waypoints", "/mqo skip keeps going past empty waypoints", false)
                .bind(OrderedWaypointManager::isSkipObstructed, OrderedWaypointManager::setSkipObstructed))
            .add(new SliderSetting("Mined-Out Threshold", "Blocks or fewer counts as mined out", 3, 0, 20, 1, " blocks")
                .bind(() -> (double) OrderedWaypointManager.getObstructedThreshold(),
                    v -> OrderedWaypointManager.setObstructedThreshold(v.intValue()))));

        Modules.register(new Module("Block Outline", "Outline blocks around the next waypoint", waypoints,
            OrderedWaypointManager::isBlockOutlineAroundWaypoint, OrderedWaypointManager::setBlockOutlineAroundWaypoint)
            .add(new BoolSetting("Outline Fill", "Tint the outlined blocks as well as edging them", false)
                .bind(OrderedWaypointManager::isBlockOutlineFill, OrderedWaypointManager::setBlockOutlineFill))
            .add(new SliderSetting("Outline Thickness", "Edge thickness", 2, 0.5, 9, 0.5, "")
                .bind(() -> (double) OrderedWaypointManager.getBlockOutlineThickness(),
                    v -> OrderedWaypointManager.setBlockOutlineThickness(v.floatValue()))));

        Modules.register(new Module("Waypoint Colors", "RGBA per waypoint type", waypoints, null, null)
            .add(ColorSetting.ofRgb("Current Waypoint", "",
                OrderedWaypointManager::getCurrentWaypointColor, OrderedWaypointManager::setCurrentWaypointColor,
                OrderedWaypointManager::getCurrentWaypointAlpha, OrderedWaypointManager::setCurrentWaypointAlpha))
            .add(ColorSetting.ofRgb("Next Waypoints", "",
                OrderedWaypointManager::getNextWaypointColor, OrderedWaypointManager::setNextWaypointColor,
                OrderedWaypointManager::getNextWaypointAlpha, OrderedWaypointManager::setNextWaypointAlpha))
            .add(ColorSetting.ofRgb("Previous Waypoint", "",
                OrderedWaypointManager::getPreviousWaypointColor, OrderedWaypointManager::setPreviousWaypointColor,
                OrderedWaypointManager::getPreviousWaypointAlpha, OrderedWaypointManager::setPreviousWaypointAlpha))
            .add(ColorSetting.ofRgb("Trace Line", "",
                OrderedWaypointManager::getTraceLineColor, OrderedWaypointManager::setTraceLineColor,
                OrderedWaypointManager::getTraceLineAlpha, OrderedWaypointManager::setTraceLineAlpha)));

        // ---- ESP ----

        Module shaft = new Module("Shaft ESP", "Littlefoot + mob highlights", esp, null, null)
            .add(new BoolSetting("Littlefoot ESP", "Highlight the Littlefoot in mineshafts", false)
                .bind(ShaftESP::isLittlefootEnabled, ShaftESP::setLittlefootEnabled))
            .add(new BoolSetting("Littlefoot Tracer", "Line from your crosshair to the Littlefoot", false)
                .bind(ShaftESP::isLittlefootTracer, ShaftESP::setLittlefootTracer))
            .add(ColorSetting.ofRgb("Littlefoot Color", "", ShaftESP::getLittlefootColor, ShaftESP::setLittlefootColor))
            .add(new BoolSetting("Mob ESP", "Highlight mineshaft mobs", false)
                .bind(ShaftESP::isMobsEnabled, ShaftESP::setMobsEnabled))
            .add(ColorSetting.ofRgb("Mob ESP Color", "",
                ShaftESP::getMobColor, ShaftESP::setMobColor,
                ShaftESP::getMobAlpha, ShaftESP::setMobAlpha));
        for (SkinMob mob : SkinMob.values()) {
            if (mob == SkinMob.LITTLEFOOT) continue;
            shaft.add(new BoolSetting(mob.displayName() + " ESP", "Highlight by skin, in the Mob ESP colour", false)
                .bind(() -> ShaftESP.isSkinMobEnabled(mob), v -> ShaftESP.setSkinMobEnabled(mob, v)));
        }
        for (ExtraMiscRows.Toggle row : ExtraEspRows.shaft) {
            shaft.add(boolOf(row));
        }
        Modules.register(shaft);

        Module corpse = new Module("Corpse ESP", "Frozen corpse waypoints", esp, null, null)
            .add(new ModeSetting("Render Mode", "Boxes, Prisma glow, or Minecraft glow",
                CorpseESP.getRenderMode().getDisplayName(), espModeNames())
                .bind(() -> CorpseESP.getRenderMode().getDisplayName(), ShatterModules::setCorpseMode))
            .add(new BoolSetting("Lapis Corpses", "Track Lapis armor corpses", false)
                .bind(CorpseESP::isLapisEnabled, v -> { if (CorpseESP.isLapisEnabled() != v) CorpseESP.toggleLapis(); }))
            .add(new BoolSetting("Tungsten Corpses", "Track Tungsten armor corpses", false)
                .bind(CorpseESP::isTungstenEnabled, v -> { if (CorpseESP.isTungstenEnabled() != v) CorpseESP.toggleTungsten(); }))
            .add(new BoolSetting("Umber Corpses", "Track Umber armor corpses", false)
                .bind(CorpseESP::isUmberEnabled, v -> { if (CorpseESP.isUmberEnabled() != v) CorpseESP.toggleUmber(); }))
            .add(new BoolSetting("Vanguard Corpses", "Track Vanguard armor corpses", false)
                .bind(CorpseESP::isVanguardEnabled, v -> { if (CorpseESP.isVanguardEnabled() != v) CorpseESP.toggleVanguard(); }));
        for (ExtraMiscRows.Toggle row : ExtraEspRows.corpse) {
            corpse.add(boolOf(row));
        }
        Modules.register(corpse);

        // Cheat/local Misc rows land on the Misc module.
        Module misc = Modules.in(general).get(1);
        for (ExtraMiscRows.Toggle row : ExtraMiscRows.toggles) {
            misc.add(boolOf(row));
        }
    }

    private static void itemTimer(Category cat, forfun.miningqol.client.ItemCooldownTimer t, String description) {
        Modules.register(new Module(t.label + " Timer", description, cat, t::isEnabled, t::setEnabled)
            .add(new BoolSetting("Hide with F1", "Hide when the vanilla HUD is hidden", false)
                .bind(t::isHideWithF1, t::setHideWithF1))
            .add(new ModeSetting("Text Align", "Where the line sits in the HUD box", "Center", "Left", "Center", "Right")
                .bind(() -> List.of("Left", "Center", "Right").get(t.getTextAlign()),
                    v -> t.setTextAlign(List.of("Left", "Center", "Right").indexOf(v))))
            .add(new BoolSetting("Seconds Suffix", "Off shows a bare number instead of 30s", true)
                .bind(t::isSecondsSuffix, t::setSecondsSuffix))
            .add(new BoolSetting("Cooldown Only", "Hide the name", false)
                .bind(t::isCooldownOnly, t::setCooldownOnly))
            .add(new SliderSetting("Cooldown", "How long the timer runs", t.getCooldownSeconds(), 1, 120, 1, "s")
                .bind(() -> (double) t.getCooldownSeconds(), v -> t.setCooldownSeconds(v.intValue())))
            .add(new SliderSetting("Scale", "HUD scale", 1.0, 0.5, 3.0, 0.05, "×")
                .bind(() -> (double) t.getScale(), v -> t.setScale(v.floatValue())))
            .add(new BoolSetting("Ready Title", "Flash a title as it comes off cooldown", false)
                .bind(t::isTitleEnabled, t::setTitleEnabled))
            .add(new SliderSetting("Title Threshold", "Seconds left when the title starts", 3, 0, 30, 1, "s")
                .bind(() -> (double) t.getTitleThreshold(), v -> t.setTitleThreshold(v.intValue())))
            .add(ColorSetting.ofRgb("Cooldown Label", "", t::getCooldownLabelColor, t::setCooldownLabelColor))
            .add(ColorSetting.ofRgb("Cooldown Value", "", t::getCooldownValueColor, t::setCooldownValueColor))
            .add(ColorSetting.ofRgb("Ready Label", "", t::getReadyLabelColor, t::setReadyLabelColor))
            .add(ColorSetting.ofRgb("Ready Value", "", t::getReadyValueColor, t::setReadyValueColor)));
    }

    /** A cheat/extra toggle row as a delegating BoolSetting. */
    public static BoolSetting boolOf(ExtraMiscRows.Toggle row) {
        return new BoolSetting(row.title, row.description, false)
            .bind(() -> row.get.invoke(), v -> row.set.invoke(v));
    }

    /** Opens an editor whose Done/Esc comes back to this menu, panels and scroll as they were. */
    private static void openScreen(java.util.function.Function<Screen, Screen> screen) {
        Screen here = Minecraft.getInstance().screen;
        Minecraft.getInstance().schedule(() -> Minecraft.getInstance().setScreen(screen.apply(here)));
    }

    private static List<String> overlayModeNames() {
        List<String> names = new ArrayList<>();
        for (BlockOverlay.Mode m : BlockOverlay.Mode.values()) names.add(m.getDisplayName());
        return names;
    }

    private static void setOverlayMode(String displayName) {
        for (BlockOverlay.Mode m : BlockOverlay.Mode.values()) {
            if (m.getDisplayName().equals(displayName)) {
                BlockOverlay.setMode(m);
                return;
            }
        }
    }

    private static List<String> styleNames() {
        List<String> names = new ArrayList<>();
        for (CommissionHUD.DisplayStyle s : CommissionHUD.DisplayStyle.values()) names.add(s.displayName);
        return names;
    }

    private static void setCommissionStyle(String displayName) {
        for (CommissionHUD.DisplayStyle s : CommissionHUD.DisplayStyle.values()) {
            if (s.displayName.equals(displayName)) {
                CommissionHUD.setDisplayStyle(s);
                return;
            }
        }
    }

    private static List<String> espModeNames() {
        List<String> names = new ArrayList<>();
        for (EntityEspMode m : EntityEspMode.values()) names.add(m.getDisplayName());
        return names;
    }

    private static void setCorpseMode(String displayName) {
        for (EntityEspMode m : EntityEspMode.values()) {
            if (m.getDisplayName().equals(displayName)) {
                CorpseESP.setRenderMode(m);
                return;
            }
        }
    }

    private static String[] shaftTypeKeys() {
        ShaftPbType[] types = ShaftPbType.values();
        String[] keys = new String[types.length];
        for (int i = 0; i < types.length; i++) keys[i] = types[i].key();
        return keys;
    }
}
