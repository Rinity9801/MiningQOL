package forfun.miningqol.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forfun.miningqol.client.BlockOverlay;
import forfun.miningqol.client.ColdTracker;
import forfun.miningqol.client.CommStatsHUD;
import forfun.miningqol.client.CommTracker;
import forfun.miningqol.client.CommandKeybindManager;
import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.CritParticleDrop;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.EntityEspMode;
import forfun.miningqol.client.FiletWarning;
import forfun.miningqol.client.LobbyFinder;
import forfun.miningqol.client.ForgeDisplay;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.party.MineshaftAutoParty;
import forfun.miningqol.client.party.PartyAutoAccept;
import forfun.miningqol.client.MayhemHUD;
import forfun.miningqol.client.WispRadius;
import forfun.miningqol.client.RollingMinerCooldown;
import forfun.miningqol.client.MqoChat;
import forfun.miningqol.client.ShaftESP;
import forfun.miningqol.client.SoundBlocker;
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MiningConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/miningqol.json");
    private static final int HUD_ANCHOR_VERSION = 2;

    /** A saved anchor mode, or -1 (pixel position only) when it was written by an older formula. */
    private int anchorMode(int mode) {
        return hudAnchorVersion >= HUD_ANCHOR_VERSION ? mode : -1;
    }

    public boolean commissionHudEnabled = true;
    public boolean commissionHudHideWithF1 = false;
    public int commissionHudX = 10;
    public int commissionHudY = 90;
    /**
     * Which formula the anchor offsets were written with; older offsets are dropped on load
     * and the pixel position adopted instead. 2 = offset to the HUD's top-left corner.
     */
    public int hudAnchorVersion = 0;
    public int commissionHudAnchorModeX = -1;
    public int commissionHudAnchorOffX = 0;
    public int commissionHudAnchorModeY = -1;
    public int commissionHudAnchorOffY = 0;
    public float commissionHudScale = 1.0f;
    public boolean commissionHudBackground = true;
    public String commissionHudLayout = "GRID";
    public String commissionHudStyle = "PANEL";
    public boolean commissionHudShowHeader = true;
    public boolean commissionHudProgressColors = false;
    public float[] commissionHeaderColor = {1.0f, 170.0f / 255.0f, 0.0f};
    public float[] commissionNameColor = {1.0f, 1.0f, 1.0f};
    public float[] commissionPercentColor = {1.0f, 1.0f, 85.0f / 255.0f};
    public float[] commissionDoneColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    public boolean commissionStatsEnabled = false;
    public boolean commissionShowOverMenus = true;
    public long commTrackTotal = 0;
    public int commStatsHudX = 10;
    public int commStatsHudY = 220;
    public int commStatsHudAnchorModeX = -1;
    public int commStatsHudAnchorOffX = 0;
    public int commStatsHudAnchorModeY = -1;
    public int commStatsHudAnchorOffY = 0;
    public float commStatsHudScale = 1.0f;

    public boolean lapisEnabled = true;
    public boolean tungstenEnabled = true;
    public boolean umberEnabled = true;
    public boolean vanguardEnabled = true;

    public boolean shaftESPEnabled = true;
    public boolean shaftESPLittlefootTracer = true;
    public boolean shaftESPMobsEnabled = false;
    public float[] shaftESPMobColor = {1.0f, 0.2f, 0.2f};
    public float shaftESPMobAlpha = 0.2f;
    public float[] shaftESPLittlefootColor = {0.0f, 1.0f, 0.4f};
    /** Skin-matched mineshaft mobs (see SkinMob): per-mob toggle, keyed by enum name. */
    public java.util.Map<String, Boolean> shaftESPSkinMobEnabled = new java.util.LinkedHashMap<>();
    public String corpseESPRenderMode = "BOX";

    public boolean blockOverlayEnabled = false;
    public String blockOverlayMode = "FILLED_OUTLINE";
    public float[] blockOverlayFillColor = {0.0f, 134.0f / 255.0f, 1.0f};
    public float blockOverlayFillAlpha = 50.0f / 255.0f;
    public float[] blockOverlayOutlineColor = {0.0f, 134.0f / 255.0f, 1.0f};
    public float blockOverlayOutlineAlpha = 1.0f;
    public float blockOverlayLineWidth = 2.5f;
    public boolean blockOverlayPhase = false;
    public boolean blockOverlayHideDuringEtherwarp = false;

    public boolean pickaxeCooldownEnabled = true;
    public boolean pickaxeCooldownHideWithF1 = false;
    public int pickaxeCooldownX = 10;
    public int pickaxeCooldownY = 50;
    public int pickaxeCooldownAnchorModeX = -1;
    public int pickaxeCooldownAnchorOffX = 0;
    public int pickaxeCooldownAnchorModeY = -1;
    public int pickaxeCooldownAnchorOffY = 0;
    public float pickaxeCooldownScale = 1.0f;
    public boolean pickaxeCooldownTitleEnabled = true;
    public int pickaxeCooldownTitleThreshold = 5;
    public boolean pickaxeCooldownCustomEnabled = false;
    public int pickaxeCooldownCustomSeconds = 120;
    public float[] pickaxeCooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    public float[] pickaxeCooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
    public float[] pickaxeReadyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    public float[] pickaxeReadyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};
    public boolean pickaxeCooldownOnly = false;
    public int pickaxeTextAlign = 1;
    public boolean pickaxeSecondsSuffix = true;
    public boolean pickaxeActiveTimerEnabled = true;
    public float[] pickaxeActiveLabelColor = {85.0f / 255.0f, 1.0f, 1.0f};
    public float[] pickaxeActiveValueColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};

    public boolean filetWarningEnabled = false;
    public boolean autoSkipShoLoad = false;
    public boolean rollingMinerCooldownEnabled = false;
    public int rollingMinerCooldownX = 10;
    public int rollingMinerCooldownY = 62;
    public int rollingMinerCooldownAnchorModeX = -1;
    public int rollingMinerCooldownAnchorOffX = 0;
    public int rollingMinerCooldownAnchorModeY = -1;
    public int rollingMinerCooldownAnchorOffY = 0;
    public float[] rollingCooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    public float[] rollingCooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
    public float[] rollingReadyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    public float[] rollingReadyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};
    /** Null until first saved, so each timer keeps its own default position. */
    public forfun.miningqol.client.ItemCooldownTimer.Saved rogueSwordTimer = null;
    public forfun.miningqol.client.ItemCooldownTimer.Saved tubaTimer = null;

    public boolean mayhemHudEnabled = false;
    public boolean mayhemHudHideWithF1 = false;
    public boolean mayhemHudAlwaysShow = false;
    public int mayhemHudX = 10;
    public int mayhemHudY = 74;
    public int mayhemHudAnchorModeX = -1;
    public int mayhemHudAnchorOffX = 0;
    public int mayhemHudAnchorModeY = -1;
    public int mayhemHudAnchorOffY = 0;
    public float[] mayhemLabelColor = {1.0f, 85.0f / 255.0f, 1.0f};
    public float[] mayhemValueColor = {1.0f, 1.0f, 85.0f / 255.0f};
    public float[] mayhemNoneColor = {170.0f / 255.0f, 170.0f / 255.0f, 170.0f / 255.0f};

    public boolean shaftSummaryEnabled = true;
    public boolean shaftSummaryFrontLoaded = false;
    public boolean shaftSummaryTrackAll = true;
    public boolean shaftSummaryShowBuffs = false;
    public boolean shaftSummaryTrackEfficiency = true;
    public int shaftSummaryEfficiencyPause = 15;
    public boolean shaftSummaryAutoSave = true;
    public boolean shaftSummaryPrintToChat = true;
    public boolean shaftSummaryPrintToParty = false;
    public int shaftSummaryMaxSavedRuns = 100;
    public java.util.List<String> shaftSummaryTypes = null;
    public java.util.List<String> shaftSummarySavedTypes = null;
    public java.util.List<String> shaftSummaryRanksBy = new java.util.ArrayList<>();
    public int shaftSummaryGemMiningSpeed = 0;
    public boolean shaftSummaryBlueCheese = false;
    public boolean shaftSummaryValueGemsAsFlawless = true;
    public boolean shaftSummarySellOffer = true;

    /** Today's Sky Mall buff and when it expires, so a relog inside the same SkyBlock day keeps it. */
    public String skyMallBuff = "";
    public long skyMallBuffUntil = 0L;

    public boolean wispRadiusEnabled = true;
    public boolean wispRadiusEdgeOnly = false;
    public boolean wispRadiusFloorBlocksOnly = true;
    public float wispRadiusRadius = 30.0f;
    public float wispRadiusAlpha = 0.35f;
    public float[] wispRadiusColor = {0.45f, 0.85f, 1.0f};
    public boolean customFogEnabled = false;
    public float customFogStart = 8.0f;
    public float customFogEnd = 48.0f;
    public float[] customFogColor = {170f / 255f, 190f / 255f, 220f / 255f};
    public float customFogAlpha = 1.0f;
    public boolean customFogSky = true;
    public boolean mineshaftPortalEnabled = true;
    public boolean mineshaftPortalTracer = true;
    public float[] mineshaftPortalColor = {170f / 255f, 1.0f, 85f / 255f};
    public float mineshaftPortalAlpha = 1.0f;
    public float mineshaftPortalLineWidth = 1.5f;

    public boolean commGuiEnabled = false;
    public boolean commGuiShowProgress = true;
    public float commGuiScale = 1.0f;
    public float commGuiButtonSize = 24.0f;
    public float[] commGuiAccentColor = {122.0f / 255.0f, 162.0f / 255.0f, 247.0f / 255.0f};
    public float[] commGuiDoneColor = {122.0f / 255.0f, 162.0f / 255.0f, 247.0f / 255.0f};

    public float shatterScale = 1.0f;
    public boolean shatterAnimations = true;
    public boolean shatterDescriptions = true;
    public double shatterOpacity = 0.65;
    public int shatterOutline = 0x99FFFFFF;
    public double shatterOutlineWidth = 1.0;
    public java.util.Map<String, int[]> shatterPanels = new java.util.HashMap<>();

    public boolean efficientMinerEnabled = false;
    public boolean useOldHeatmap = false;

    public java.util.List<String> lobbyFinderBlocks = new java.util.ArrayList<>();
    public java.util.Map<String, String> commandKeybinds = new java.util.HashMap<>();

    public boolean forgeDisplayEnabled = false;
    public boolean forgeDisplayHideWithF1 = false;
    public boolean forgeDisplayShowEmpty = false;
    public boolean forgeDisplaySortByTime = true;
    public int forgeDisplayX = 10;
    public int forgeDisplayY = 90;
    public int forgeDisplayAnchorModeX = -1;
    public int forgeDisplayAnchorOffX = 0;
    public int forgeDisplayAnchorModeY = -1;
    public int forgeDisplayAnchorOffY = 0;
    public float[] forgeTitleColor = {1.0f, 170.0f / 255.0f, 0.0f};
    public float[] forgeItemColor = {1.0f, 1.0f, 1.0f};
    public float[] forgeTimeColor = {170.0f / 255.0f, 170.0f / 255.0f, 170.0f / 255.0f};
    public float[] forgeReadyColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};

    public boolean autoPartyEnabled = false;
    public boolean autoPartyDisbandAfterWarp = true;
    public int autoPartyDisbandSeconds = 10;
    public float autoPartyWarpDelaySeconds = 5f;
    public boolean autoPartyDisbandOnTimeout = true;
    public int autoPartySettleSeconds = 5;
    public boolean autoPartyAcceptEnabled = false;
    public boolean autoPartyAcceptBlockAbility = true;
    public boolean autoPartyAcceptBlockInShaft = true;
    public boolean autoPartyAcceptBlockWhenReady = true;
    public java.util.List<String> autoPartyAcceptList = new java.util.ArrayList<>();
    /** Players who want any shaft where the ESP spots a Littlefoot. */
    public java.util.List<String> autoPartyLittlefootMob = new java.util.ArrayList<>();
    /** Player name -> shaft types they must never be warped to. */
    public java.util.Map<String, java.util.List<String>> autoPartyBlockedSignups = new java.util.LinkedHashMap<>();
    /** Players switched off in the manager: kept with their picks, never invited. */
    public java.util.List<String> autoPartyDisabledPlayers = new java.util.ArrayList<>();
    /** Player name -> the ShaftType names they are signed up for. */
    public java.util.Map<String, java.util.List<String>> autoPartySignups = new java.util.LinkedHashMap<>();
    /** Player name -> "CORPSE:COUNT" picks, e.g. "LAPIS:3". */
    public java.util.Map<String, java.util.List<String>> autoPartyCorpseSignups = new java.util.LinkedHashMap<>();


    public boolean chatLogsEnabled = true;
    public boolean critParticleDrop = false;

    public boolean soundBlockingEnabled = true;
    public java.util.List<String> soundBlockRules = new java.util.ArrayList<>();

    // Cheat-only fields (plain data; applied via CheatHooks on -cheat builds,
    // harmlessly ignored on legit)
    public int autoClickerMiningSlot = 0;
    public boolean autoClickerSecondDrill = false;
    /** CoalClick: swap to the hotbar fishing rod and right-click it before the ability. */
    public boolean autoClickerRodSwap = false;
    public int autoClickerSecondDrillSlot = 3;
    public boolean autoClickerHudEnabled = true;
    public int autoClickerMainDrillDelay = 3;
    public int autoClickerSecondDrillDelay = 3;
    public boolean autoClickerCustomCooldown = false;
    public int autoClickerCustomCooldownSeconds = 120;
    public int coldClickerMiningSlot = 0;
    public int coldClickerSecondDrillSlot = 3;
    public boolean coldClickerThirdDrillEnabled = false;
    public int coldClickerThirdDrillSlot = 4;
    public int coldClickerMainDrillDelay = 3;
    public int coldClickerSecondDrillDelay = 3;
    public int coldClickerColdThreshold = 50;
    public boolean coldClickerShowToggleMessage = true;
    public int shaftClickerMiningSlot = 0;
    public boolean shaftClickerShowToggleMessage = true;
    public int commClaimBatPersonSlot = 1;
    public int commClaimDivanSlot = 2;
    public int commClaimRefinedToolSlot = 0;
    public int commClaimTickDelay = 2;
    public int commClaimGuiWaitDelay = 10;
    public boolean commClaimAutoTrigger = false;
    public boolean commClaimWardrobeSwap = true;
    public boolean commClaimBatchMining = true;
    /** Open commissions with the Royal Pigeon (true) or by calling Mismyla on the Abiphone (false). */
    public boolean commClaimUsePigeon = false;
    /** How long the pigeon is held before its right-click, in ms. */
    public int commClaimPigeonHoldMs = 100;
    /** How long the pigeon stays in hand after its right-click, in ms. */
    public int commClaimPigeonReleaseMs = 25;
    public boolean commClaimBlockInput = true;
    public boolean commClaimHideGui = false;
    public String emptyStashMaterial = "COAL";
    public int emptyStashDelay = 4;
    public boolean autoForgeEnabled = true;
    public int autoForgeTickDelay = 3;
    public int autoForgeRunCount = 1;
    /** Custom crafts as "label|category|needle". */
    /** Crafts recorded by clicking through The Forge: "label|title>item>slot;title>item>slot…". */
    public java.util.List<String> autoForgeRecordedCrafts = new java.util.ArrayList<>();
    /** Built-in Auto Forge crafts hidden from the picker, by label. */
    public java.util.List<String> autoForgeHiddenCrafts = new java.util.ArrayList<>();
    public boolean shaftJoinCdEnabled = true;
    public int shaftJoinCdSeconds = 30;


    public boolean orderedWaypointsEnabled = true;
    public float orderedWaypointRange = 4.5f;
    public int orderedWaypointNextCount = 2;
    public boolean orderedWaypointTraceLine = true;
    public boolean orderedWaypointShowDistance = true;
    public boolean orderedWaypointShowName = true;
    public float[] orderedWaypointCurrentColor = {85f/255f, 1f, 85f/255f};
    public float[] orderedWaypointNextColor = {1f, 1f, 85f/255f};
    public float[] orderedWaypointPreviousColor = {85f/255f, 85f/255f, 1f};
    public float[] orderedWaypointTraceLineColor = {85f/255f, 1f, 85f/255f};
    public float orderedWaypointCurrentAlpha = 0.6f;
    public float orderedWaypointNextAlpha = 0.6f;
    public float orderedWaypointPreviousAlpha = 0.6f;
    public float orderedWaypointTraceLineAlpha = 1f;
    public boolean orderedWaypointLobbyCheckEnabled = false;
    public String orderedWaypointLobbyCheckBlock = "minecraft:coal_ore";
    public int orderedWaypointLobbyCheckInterval = 10;
    public int orderedWaypointLobbyCheckRadius = 2;
    public boolean orderedWaypointBlockOutline = false;
    public int orderedWaypointBlockOutlineRadius = 3;
    public float[] orderedWaypointBlockOutlineColor = {1f, 1f, 1f};
    public float orderedWaypointBlockOutlineAlpha = 0.8f;
    /** Outline edge half-thickness in blocks. 1.5 matches how 1.21.11's GL lines looked. */
    public float orderedWaypointBlockOutlineThickness = 1.5f;
    /** Tint the outlined blocks as well as edging them. */
    public boolean orderedWaypointBlockOutlineFill = true;
    /** Cheat builds: auto right-click when sneaking + aiming at an etherwarp-marked waypoint. */
    public boolean orderedWaypointEtherwarpClick = true;
    /** /mqo skip walks past waypoints whose lobby-check block is gone. */
    public boolean orderedWaypointSkipObstructed = false;
    /** At or below this many blocks near a waypoint, /mqo skip treats it as mined out. */
    public int orderedWaypointObstructedThreshold = 5;

    public static MiningConfig load() {
        if (!CONFIG_FILE.exists()) {
            MiningConfig config = new MiningConfig();
            config.ensureDefaults();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            MiningConfig config = GSON.fromJson(reader, MiningConfig.class);
            if (config == null) {
                config = new MiningConfig();
            }
            config.ensureDefaults();
            return config;
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to load config", e);
            MiningConfig config = new MiningConfig();
            config.ensureDefaults();
            return config;
        }
    }

    private void ensureDefaults() {
        if (skyMallBuff == null) skyMallBuff = "";
        if (wispRadiusColor == null || wispRadiusColor.length != 3) wispRadiusColor = new float[]{0.45f, 0.85f, 1.0f};
        if (mineshaftPortalColor == null || mineshaftPortalColor.length != 3) mineshaftPortalColor = new float[]{170f / 255f, 1.0f, 85f / 255f};
        if (customFogColor == null || customFogColor.length != 3) customFogColor = new float[]{170f / 255f, 190f / 255f, 220f / 255f};
        if (wispRadiusRadius < 1f || wispRadiusRadius > 64f) wispRadiusRadius = 30.0f;
        if (wispRadiusAlpha < 0.05f || wispRadiusAlpha > 1f) wispRadiusAlpha = 0.35f;
        if (shaftSummaryEfficiencyPause < 1 || shaftSummaryEfficiencyPause > 120) shaftSummaryEfficiencyPause = 15;
        if (shaftSummaryMaxSavedRuns < 0 || shaftSummaryMaxSavedRuns > 1000) shaftSummaryMaxSavedRuns = 100;
        // Null = never written by this build: Jeff's defaults (every type summarised, only Jasper saved).
        if (shaftSummaryTypes == null) shaftSummaryTypes = new java.util.ArrayList<>(forfun.miningqol.client.summary.ShaftSummary.defaultSummaryKeys());
        if (shaftSummarySavedTypes == null) shaftSummarySavedTypes = new java.util.ArrayList<>(forfun.miningqol.client.summary.ShaftSummary.defaultSavedKeys());
        if (shaftSummaryRanksBy == null) shaftSummaryRanksBy = new java.util.ArrayList<>();
        if (commissionHudScale < 0.5f || commissionHudScale > 2.0f) {
            commissionHudScale = 1.0f;
        }
        if (commStatsHudScale < 0.5f || commStatsHudScale > 2.0f) {
            commStatsHudScale = 1.0f;
        }
        if (commissionHudStyle == null) commissionHudStyle = "PANEL";
        if (commissionHeaderColor == null || commissionHeaderColor.length < 3) commissionHeaderColor = new float[]{1.0f, 170.0f / 255.0f, 0.0f};
        if (commissionNameColor == null || commissionNameColor.length < 3) commissionNameColor = new float[]{1.0f, 1.0f, 1.0f};
        if (commissionPercentColor == null || commissionPercentColor.length < 3) commissionPercentColor = new float[]{1.0f, 1.0f, 85.0f / 255.0f};
        if (commissionDoneColor == null || commissionDoneColor.length < 3) commissionDoneColor = new float[]{85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
        if (commissionHudLayout == null) {
            commissionHudLayout = "GRID";
        }
        if (orderedWaypointCurrentColor == null) orderedWaypointCurrentColor = new float[]{85f/255f, 1f, 85f/255f};
        if (orderedWaypointNextColor == null) orderedWaypointNextColor = new float[]{1f, 1f, 85f/255f};
        if (orderedWaypointPreviousColor == null) orderedWaypointPreviousColor = new float[]{85f/255f, 85f/255f, 1f};
        if (orderedWaypointTraceLineColor == null) orderedWaypointTraceLineColor = new float[]{85f/255f, 1f, 85f/255f};
        if (shaftESPMobColor == null) shaftESPMobColor = new float[]{1.0f, 0.2f, 0.2f};
        if (shaftESPLittlefootColor == null || shaftESPLittlefootColor.length < 3) shaftESPLittlefootColor = new float[]{0.0f, 1.0f, 0.4f};
        if (shaftESPSkinMobEnabled == null) shaftESPSkinMobEnabled = new java.util.LinkedHashMap<>();
        if (corpseESPRenderMode == null) corpseESPRenderMode = "BOX";
        if (blockOverlayMode == null) blockOverlayMode = "FILLED_OUTLINE";
        if (blockOverlayFillColor == null) blockOverlayFillColor = new float[]{0.0f, 134.0f / 255.0f, 1.0f};
        if (blockOverlayOutlineColor == null) blockOverlayOutlineColor = new float[]{0.0f, 134.0f / 255.0f, 1.0f};
        if (pickaxeCooldownLabelColor == null || pickaxeCooldownLabelColor.length < 3) pickaxeCooldownLabelColor = new float[]{1.0f, 170.0f / 255.0f, 0.0f};
        if (pickaxeCooldownValueColor == null || pickaxeCooldownValueColor.length < 3) pickaxeCooldownValueColor = new float[]{1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
        if (pickaxeReadyLabelColor == null || pickaxeReadyLabelColor.length < 3) pickaxeReadyLabelColor = new float[]{85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
        if (pickaxeReadyValueColor == null || pickaxeReadyValueColor.length < 3) pickaxeReadyValueColor = new float[]{0.0f, 170.0f / 255.0f, 0.0f};
        if (pickaxeActiveLabelColor == null || pickaxeActiveLabelColor.length < 3) pickaxeActiveLabelColor = new float[]{85.0f / 255.0f, 1.0f, 1.0f};
        if (pickaxeActiveValueColor == null || pickaxeActiveValueColor.length < 3) pickaxeActiveValueColor = new float[]{85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
        if (rollingCooldownLabelColor == null || rollingCooldownLabelColor.length < 3) rollingCooldownLabelColor = new float[]{1.0f, 170.0f / 255.0f, 0.0f};
        if (rollingCooldownValueColor == null || rollingCooldownValueColor.length < 3) rollingCooldownValueColor = new float[]{1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
        if (rollingReadyLabelColor == null || rollingReadyLabelColor.length < 3) rollingReadyLabelColor = new float[]{85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
        if (rollingReadyValueColor == null || rollingReadyValueColor.length < 3) rollingReadyValueColor = new float[]{0.0f, 170.0f / 255.0f, 0.0f};
        if (mayhemLabelColor == null || mayhemLabelColor.length < 3) mayhemLabelColor = new float[]{1.0f, 85.0f / 255.0f, 1.0f};
        if (mayhemValueColor == null || mayhemValueColor.length < 3) mayhemValueColor = new float[]{1.0f, 1.0f, 85.0f / 255.0f};
        if (mayhemNoneColor == null || mayhemNoneColor.length < 3) mayhemNoneColor = new float[]{170.0f / 255.0f, 170.0f / 255.0f, 170.0f / 255.0f};
        if (commGuiAccentColor == null || commGuiAccentColor.length < 3) commGuiAccentColor = new float[]{122.0f / 255.0f, 162.0f / 255.0f, 247.0f / 255.0f};
        if (commGuiDoneColor == null || commGuiDoneColor.length < 3) commGuiDoneColor = new float[]{122.0f / 255.0f, 162.0f / 255.0f, 247.0f / 255.0f};
        if (shatterPanels == null) shatterPanels = new java.util.HashMap<>();
        if (orderedWaypointBlockOutlineColor == null) orderedWaypointBlockOutlineColor = new float[]{1f, 1f, 1f};
        if (emptyStashMaterial == null) emptyStashMaterial = "COAL";
        if (orderedWaypointLobbyCheckBlock == null) orderedWaypointLobbyCheckBlock = "minecraft:coal_ore";
        if (lobbyFinderBlocks == null) lobbyFinderBlocks = new java.util.ArrayList<>();
        if (commandKeybinds == null) commandKeybinds = new java.util.HashMap<>();
        if (forgeTitleColor == null || forgeTitleColor.length < 3) forgeTitleColor = new float[]{1.0f, 170.0f / 255.0f, 0.0f};
        if (forgeItemColor == null || forgeItemColor.length < 3) forgeItemColor = new float[]{1.0f, 1.0f, 1.0f};
        if (forgeTimeColor == null || forgeTimeColor.length < 3) forgeTimeColor = new float[]{170.0f / 255.0f, 170.0f / 255.0f, 170.0f / 255.0f};
        if (forgeReadyColor == null || forgeReadyColor.length < 3) forgeReadyColor = new float[]{85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
        if (autoPartySignups == null) autoPartySignups = new java.util.LinkedHashMap<>();
        if (autoPartyCorpseSignups == null) autoPartyCorpseSignups = new java.util.LinkedHashMap<>();
        if (autoPartyAcceptList == null) autoPartyAcceptList = new java.util.ArrayList<>();
        if (autoPartyLittlefootMob == null) autoPartyLittlefootMob = new java.util.ArrayList<>();
        if (autoPartyBlockedSignups == null) autoPartyBlockedSignups = new java.util.LinkedHashMap<>();
        if (autoPartyDisabledPlayers == null) autoPartyDisabledPlayers = new java.util.ArrayList<>();
        if (autoForgeRecordedCrafts == null) autoForgeRecordedCrafts = new java.util.ArrayList<>();
        if (autoForgeHiddenCrafts == null) autoForgeHiddenCrafts = new java.util.ArrayList<>();
        if (soundBlockRules == null) soundBlockRules = new java.util.ArrayList<>();
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to save config", e);
        }
    }

    public void applyToGame() {
        CommissionHUD.setEnabled(commissionHudEnabled);
        CommissionHUD.setHideWithF1(commissionHudHideWithF1);
        CommissionHUD.setPosition(commissionHudX, commissionHudY);
        CommissionHUD.anchor().load(anchorMode(commissionHudAnchorModeX), commissionHudAnchorOffX, anchorMode(commissionHudAnchorModeY), commissionHudAnchorOffY);
        CommissionHUD.setScale(commissionHudScale);
        CommissionHUD.setBackgroundEnabled(commissionHudBackground);
        try {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.valueOf(commissionHudLayout));
        } catch (Exception e) {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.GRID);
        }
        try {
            CommissionHUD.setDisplayStyle(CommissionHUD.DisplayStyle.valueOf(commissionHudStyle));
        } catch (Exception e) {
            CommissionHUD.setDisplayStyle(CommissionHUD.DisplayStyle.PANEL);
        }
        CommissionHUD.setShowHeader(commissionHudShowHeader);
        CommissionHUD.setProgressColors(commissionHudProgressColors);
        CommissionHUD.setHeaderColor(commissionHeaderColor[0], commissionHeaderColor[1], commissionHeaderColor[2]);
        CommissionHUD.setNameColor(commissionNameColor[0], commissionNameColor[1], commissionNameColor[2]);
        CommissionHUD.setPercentColor(commissionPercentColor[0], commissionPercentColor[1], commissionPercentColor[2]);
        CommissionHUD.setDoneColor(commissionDoneColor[0], commissionDoneColor[1], commissionDoneColor[2]);
        CommTracker.setStatsEnabled(commissionStatsEnabled);
        CommissionHUD.setShowOverMenus(commissionShowOverMenus);
        CommTracker.setTotalCompleted(commTrackTotal);
        CommStatsHUD.setPosition(commStatsHudX, commStatsHudY);
        CommStatsHUD.anchor().load(anchorMode(commStatsHudAnchorModeX), commStatsHudAnchorOffX, anchorMode(commStatsHudAnchorModeY), commStatsHudAnchorOffY);
        CommStatsHUD.setScale(commStatsHudScale);

        if (CorpseESP.isLapisEnabled() != lapisEnabled) CorpseESP.toggleLapis();
        if (CorpseESP.isTungstenEnabled() != tungstenEnabled) CorpseESP.toggleTungsten();
        if (CorpseESP.isUmberEnabled() != umberEnabled) CorpseESP.toggleUmber();
        if (CorpseESP.isVanguardEnabled() != vanguardEnabled) CorpseESP.toggleVanguard();

        ShaftESP.setLittlefootEnabled(shaftESPEnabled);
        ShaftESP.setLittlefootTracer(shaftESPLittlefootTracer);
        ShaftESP.setMobsEnabled(shaftESPMobsEnabled);
        ShaftESP.setMobColor(shaftESPMobColor[0], shaftESPMobColor[1], shaftESPMobColor[2]);
        ShaftESP.setMobAlpha(shaftESPMobAlpha);
        for (forfun.miningqol.client.SkinMob mob : forfun.miningqol.client.SkinMob.values()) {
            if (mob == forfun.miningqol.client.SkinMob.LITTLEFOOT) continue;   // shaftESPEnabled is its toggle
            Boolean on = shaftESPSkinMobEnabled.get(mob.name());
            if (on != null) mob.setEnabled(on);
        }
        ShaftESP.setLittlefootColor(shaftESPLittlefootColor[0], shaftESPLittlefootColor[1], shaftESPLittlefootColor[2]);
        try {
            CorpseESP.setRenderMode(EntityEspMode.valueOf(corpseESPRenderMode));
        } catch (IllegalArgumentException e) {
            CorpseESP.setRenderMode(EntityEspMode.BOX);
        }

        BlockOverlay.setEnabled(blockOverlayEnabled);
        try {
            BlockOverlay.setMode(BlockOverlay.Mode.valueOf(blockOverlayMode));
        } catch (IllegalArgumentException e) {
            BlockOverlay.setMode(BlockOverlay.Mode.FILLED_OUTLINE);
        }
        BlockOverlay.setFillColor(blockOverlayFillColor[0], blockOverlayFillColor[1], blockOverlayFillColor[2]);
        BlockOverlay.setFillAlpha(blockOverlayFillAlpha);
        BlockOverlay.setOutlineColor(blockOverlayOutlineColor[0], blockOverlayOutlineColor[1], blockOverlayOutlineColor[2]);
        BlockOverlay.setOutlineAlpha(blockOverlayOutlineAlpha);
        BlockOverlay.setLineWidth(blockOverlayLineWidth);
        BlockOverlay.setPhase(blockOverlayPhase);
        BlockOverlay.setHideDuringEtherwarp(blockOverlayHideDuringEtherwarp);

        PickaxeCooldownHUD.setEnabled(pickaxeCooldownEnabled);
        PickaxeCooldownHUD.setHideWithF1(pickaxeCooldownHideWithF1);
        PickaxeCooldownHUD.setPosition(pickaxeCooldownX, pickaxeCooldownY);
        PickaxeCooldownHUD.anchor().load(anchorMode(pickaxeCooldownAnchorModeX), pickaxeCooldownAnchorOffX, anchorMode(pickaxeCooldownAnchorModeY), pickaxeCooldownAnchorOffY);
        PickaxeCooldownHUD.setScale(pickaxeCooldownScale);
        PickaxeCooldownHUD.setTitleEnabled(pickaxeCooldownTitleEnabled);
        PickaxeCooldownHUD.setTitleThreshold(pickaxeCooldownTitleThreshold);
        PickaxeCooldownHUD.setCustomCooldownSeconds(pickaxeCooldownCustomSeconds);
        PickaxeCooldownHUD.setCustomCooldownEnabled(pickaxeCooldownCustomEnabled);
        PickaxeCooldownHUD.setCooldownLabelColor(pickaxeCooldownLabelColor[0], pickaxeCooldownLabelColor[1], pickaxeCooldownLabelColor[2]);
        PickaxeCooldownHUD.setCooldownValueColor(pickaxeCooldownValueColor[0], pickaxeCooldownValueColor[1], pickaxeCooldownValueColor[2]);
        PickaxeCooldownHUD.setReadyLabelColor(pickaxeReadyLabelColor[0], pickaxeReadyLabelColor[1], pickaxeReadyLabelColor[2]);
        PickaxeCooldownHUD.setReadyValueColor(pickaxeReadyValueColor[0], pickaxeReadyValueColor[1], pickaxeReadyValueColor[2]);
        PickaxeCooldownHUD.setCooldownOnly(pickaxeCooldownOnly);
        PickaxeCooldownHUD.setTextAlign(pickaxeTextAlign);
        PickaxeCooldownHUD.setSecondsSuffix(pickaxeSecondsSuffix);
        PickaxeCooldownHUD.setActiveTimerEnabled(pickaxeActiveTimerEnabled);
        PickaxeCooldownHUD.setActiveLabelColor(pickaxeActiveLabelColor[0], pickaxeActiveLabelColor[1], pickaxeActiveLabelColor[2]);
        PickaxeCooldownHUD.setActiveValueColor(pickaxeActiveValueColor[0], pickaxeActiveValueColor[1], pickaxeActiveValueColor[2]);

        FiletWarning.setEnabled(filetWarningEnabled);
        RollingMinerCooldown.setPosition(rollingMinerCooldownX, rollingMinerCooldownY);
        RollingMinerCooldown.anchor().load(anchorMode(rollingMinerCooldownAnchorModeX), rollingMinerCooldownAnchorOffX, anchorMode(rollingMinerCooldownAnchorModeY), rollingMinerCooldownAnchorOffY);
        RollingMinerCooldown.setEnabled(rollingMinerCooldownEnabled);
        if (rogueSwordTimer != null) forfun.miningqol.client.ItemCooldownTimer.ROGUE_SWORD.load(rogueSwordTimer,
            anchorMode(rogueSwordTimer.anchorModeX), anchorMode(rogueSwordTimer.anchorModeY));
        if (tubaTimer != null) forfun.miningqol.client.ItemCooldownTimer.TUBA.load(tubaTimer,
            anchorMode(tubaTimer.anchorModeX), anchorMode(tubaTimer.anchorModeY));
        RollingMinerCooldown.setCooldownLabelColor(rollingCooldownLabelColor[0], rollingCooldownLabelColor[1], rollingCooldownLabelColor[2]);
        RollingMinerCooldown.setCooldownValueColor(rollingCooldownValueColor[0], rollingCooldownValueColor[1], rollingCooldownValueColor[2]);
        RollingMinerCooldown.setReadyLabelColor(rollingReadyLabelColor[0], rollingReadyLabelColor[1], rollingReadyLabelColor[2]);
        RollingMinerCooldown.setReadyValueColor(rollingReadyValueColor[0], rollingReadyValueColor[1], rollingReadyValueColor[2]);

        forfun.miningqol.client.summary.ShaftSummary.setEnabled(shaftSummaryEnabled);
        forfun.miningqol.client.summary.ShaftSummary.setFrontLoaded(shaftSummaryFrontLoaded);
        forfun.miningqol.client.summary.ShaftSummary.setTrackAllShaftSummaries(shaftSummaryTrackAll);
        forfun.miningqol.client.summary.ShaftSummary.setShowBuffsInSummary(shaftSummaryShowBuffs);
        forfun.miningqol.client.summary.ShaftSummary.setTrackEfficiencyInSummary(shaftSummaryTrackEfficiency);
        forfun.miningqol.client.summary.ShaftSummary.setEfficiencyPauseSeconds(shaftSummaryEfficiencyPause);
        forfun.miningqol.client.summary.ShaftSummary.setAutoSave(shaftSummaryAutoSave);
        forfun.miningqol.client.summary.ShaftSummary.setPrintToChat(shaftSummaryPrintToChat);
        forfun.miningqol.client.summary.ShaftSummary.setPrintToParty(shaftSummaryPrintToParty);
        forfun.miningqol.client.summary.ShaftSummary.setMaxSavedRuns(shaftSummaryMaxSavedRuns);
        forfun.miningqol.client.summary.ShaftSummary.setSummaryShaftTypes(shaftSummaryTypes);
        forfun.miningqol.client.summary.ShaftSummary.setSavedShaftTypes(shaftSummarySavedTypes);
        forfun.miningqol.client.summary.ShaftSummary.setRanksBy(shaftSummaryRanksBy);
        forfun.miningqol.client.summary.ShaftSummary.setGemMiningSpeed(shaftSummaryGemMiningSpeed);
        forfun.miningqol.client.summary.ShaftSummary.setBlueCheese(shaftSummaryBlueCheese);
        forfun.miningqol.client.summary.ShaftSummary.setValueAsFlawless(shaftSummaryValueGemsAsFlawless);
        forfun.miningqol.client.summary.ShaftSummary.setSellOffer(shaftSummarySellOffer);

        forfun.miningqol.client.summary.SkyMallTracker.restore(skyMallBuff, skyMallBuffUntil);

        WispRadius.setEnabled(wispRadiusEnabled);
        WispRadius.setEdgeOnly(wispRadiusEdgeOnly);
        WispRadius.setFloorBlocksOnly(wispRadiusFloorBlocksOnly);
        WispRadius.setRadius(wispRadiusRadius);
        WispRadius.setAlpha(wispRadiusAlpha);
        WispRadius.setColor(wispRadiusColor[0], wispRadiusColor[1], wispRadiusColor[2]);
        forfun.miningqol.client.CustomFog.setEnabled(customFogEnabled);
        forfun.miningqol.client.CustomFog.setStart(customFogStart);
        forfun.miningqol.client.CustomFog.setEnd(customFogEnd);
        forfun.miningqol.client.CustomFog.setColor(customFogColor[0], customFogColor[1], customFogColor[2]);
        forfun.miningqol.client.CustomFog.setAlpha(customFogAlpha);
        forfun.miningqol.client.CustomFog.setFogSky(customFogSky);
        forfun.miningqol.client.MineshaftPortal.setEnabled(mineshaftPortalEnabled);
        forfun.miningqol.client.MineshaftPortal.setTracer(mineshaftPortalTracer);
        forfun.miningqol.client.MineshaftPortal.setColor(mineshaftPortalColor[0], mineshaftPortalColor[1], mineshaftPortalColor[2]);
        forfun.miningqol.client.MineshaftPortal.setAlpha(mineshaftPortalAlpha);
        forfun.miningqol.client.MineshaftPortal.setLineWidth(mineshaftPortalLineWidth);

        MayhemHUD.setEnabled(mayhemHudEnabled);
        MayhemHUD.setHideWithF1(mayhemHudHideWithF1);
        MayhemHUD.setAlwaysShow(mayhemHudAlwaysShow);
        MayhemHUD.setPosition(mayhemHudX, mayhemHudY);
        MayhemHUD.anchor().load(anchorMode(mayhemHudAnchorModeX), mayhemHudAnchorOffX, anchorMode(mayhemHudAnchorModeY), mayhemHudAnchorOffY);
        MayhemHUD.setLabelColor(mayhemLabelColor[0], mayhemLabelColor[1], mayhemLabelColor[2]);
        MayhemHUD.setValueColor(mayhemValueColor[0], mayhemValueColor[1], mayhemValueColor[2]);
        MayhemHUD.setNoneColor(mayhemNoneColor[0], mayhemNoneColor[1], mayhemNoneColor[2]);

        forfun.miningqol.client.CommissionGui.setEnabled(commGuiEnabled);
        forfun.miningqol.client.CommissionGui.setShowProgress(commGuiShowProgress);
        forfun.miningqol.client.CommissionGui.setScale(commGuiScale);
        forfun.miningqol.client.CommissionGui.setButtonSize(commGuiButtonSize);
        forfun.miningqol.client.CommissionGui.setAccentColor(commGuiAccentColor[0], commGuiAccentColor[1], commGuiAccentColor[2]);
        forfun.miningqol.client.CommissionGui.setDoneColor(commGuiDoneColor[0], commGuiDoneColor[1], commGuiDoneColor[2]);

        forfun.miningqol.client.shatter.ShatterUi.setScale(shatterScale);
        forfun.miningqol.client.shatter.ShatterUi.setAnimations(shatterAnimations);
        forfun.miningqol.client.shatter.ShatterUi.setDescriptions(shatterDescriptions);
        forfun.miningqol.client.shatter.ShatterUi.opacity.set(shatterOpacity);
        forfun.miningqol.client.shatter.ShatterUi.outline.set(shatterOutline);
        forfun.miningqol.client.shatter.ShatterUi.outlineWidth.set(shatterOutlineWidth);
        forfun.miningqol.client.shatter.ShatterConfig.load(shatterPanels);

        EfficientMinerOverlay.setEnabled(efficientMinerEnabled);
        EfficientMinerOverlay.setUseOldHeatmap(useOldHeatmap);

        ForgeDisplay.setEnabled(forgeDisplayEnabled);
        ForgeDisplay.setHideWithF1(forgeDisplayHideWithF1);
        ForgeDisplay.setShowEmpty(forgeDisplayShowEmpty);
        ForgeDisplay.setSortByTime(forgeDisplaySortByTime);
        ForgeDisplay.setPosition(forgeDisplayX, forgeDisplayY);
        ForgeDisplay.anchor().load(anchorMode(forgeDisplayAnchorModeX), forgeDisplayAnchorOffX, anchorMode(forgeDisplayAnchorModeY), forgeDisplayAnchorOffY);
        ForgeDisplay.setTitleColor(forgeTitleColor[0], forgeTitleColor[1], forgeTitleColor[2]);
        ForgeDisplay.setItemColor(forgeItemColor[0], forgeItemColor[1], forgeItemColor[2]);
        ForgeDisplay.setTimeColor(forgeTimeColor[0], forgeTimeColor[1], forgeTimeColor[2]);
        ForgeDisplay.setReadyColor(forgeReadyColor[0], forgeReadyColor[1], forgeReadyColor[2]);

        MineshaftAutoParty.setDisbandAfterWarp(autoPartyDisbandAfterWarp);
        MineshaftAutoParty.setDisbandSeconds(autoPartyDisbandSeconds);
        MineshaftAutoParty.setWarpDelaySeconds(autoPartyWarpDelaySeconds);
        MineshaftAutoParty.setDisbandOnTimeout(autoPartyDisbandOnTimeout);
        MineshaftAutoParty.setSettleSeconds(autoPartySettleSeconds);
        PartyAutoAccept.setNames(autoPartyAcceptList);
        PartyAutoAccept.setBlockDuringAbility(autoPartyAcceptBlockAbility);
        PartyAutoAccept.setBlockInShaft(autoPartyAcceptBlockInShaft);
        PartyAutoAccept.setBlockWhenReady(autoPartyAcceptBlockWhenReady);
        PartyAutoAccept.setEnabled(autoPartyAcceptEnabled);
        MineshaftAutoParty.importSignups(autoPartySignups, autoPartyCorpseSignups, autoPartyLittlefootMob,
            autoPartyBlockedSignups, autoPartyDisabledPlayers);
        // Last: setEnabled(false) aborts any in-flight party, so it must see the final state.
        MineshaftAutoParty.setEnabled(autoPartyEnabled);

        CommandKeybindManager.clearAll();
        for (java.util.Map.Entry<String, String> entry : commandKeybinds.entrySet()) {
            try {
                CommandKeybindManager.registerKeybind(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {}
        }

        java.util.Set<net.minecraft.core.BlockPos> blocks = new java.util.HashSet<>();
        for (String posStr : lobbyFinderBlocks) {
            try {
                String[] parts = posStr.split(",");
                if (parts.length == 3) {
                    blocks.add(new net.minecraft.core.BlockPos(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                }
            } catch (NumberFormatException ignored) {}
        }
        LobbyFinder.setTrackedBlocks(blocks);

        MqoChat.setLogsEnabled(chatLogsEnabled);
        CritParticleDrop.setEnabled(critParticleDrop);

        SoundBlocker.setBlockingEnabled(soundBlockingEnabled);
        SoundBlocker.setRules(soundBlockRules);

        if (forfun.miningqol.client.CheatHooks.applyConfig != null) {
            forfun.miningqol.client.CheatHooks.applyConfig.run();
        }

        OrderedWaypointManager.setEnabled(orderedWaypointsEnabled);
        OrderedWaypointManager.setWaypointRange(orderedWaypointRange);
        OrderedWaypointManager.setNextCount(orderedWaypointNextCount);
        OrderedWaypointManager.setTraceLineEnabled(orderedWaypointTraceLine);
        OrderedWaypointManager.setShowDistance(orderedWaypointShowDistance);
        OrderedWaypointManager.setShowName(orderedWaypointShowName);
        OrderedWaypointManager.setCurrentWaypointColor(orderedWaypointCurrentColor[0], orderedWaypointCurrentColor[1], orderedWaypointCurrentColor[2]);
        OrderedWaypointManager.setNextWaypointColor(orderedWaypointNextColor[0], orderedWaypointNextColor[1], orderedWaypointNextColor[2]);
        OrderedWaypointManager.setPreviousWaypointColor(orderedWaypointPreviousColor[0], orderedWaypointPreviousColor[1], orderedWaypointPreviousColor[2]);
        OrderedWaypointManager.setTraceLineColor(orderedWaypointTraceLineColor[0], orderedWaypointTraceLineColor[1], orderedWaypointTraceLineColor[2]);
        OrderedWaypointManager.setCurrentWaypointAlpha(orderedWaypointCurrentAlpha);
        OrderedWaypointManager.setNextWaypointAlpha(orderedWaypointNextAlpha);
        OrderedWaypointManager.setPreviousWaypointAlpha(orderedWaypointPreviousAlpha);
        OrderedWaypointManager.setTraceLineAlpha(orderedWaypointTraceLineAlpha);
        OrderedWaypointManager.setLobbyCheckEnabled(orderedWaypointLobbyCheckEnabled);
        OrderedWaypointManager.setLobbyCheckBlock(orderedWaypointLobbyCheckBlock);
        OrderedWaypointManager.setLobbyCheckInterval(orderedWaypointLobbyCheckInterval);
        OrderedWaypointManager.setLobbyCheckRadius(orderedWaypointLobbyCheckRadius);
        OrderedWaypointManager.setBlockOutlineAroundWaypoint(orderedWaypointBlockOutline);
        OrderedWaypointManager.setBlockOutlineRadius(orderedWaypointBlockOutlineRadius);
        OrderedWaypointManager.setBlockOutlineColor(orderedWaypointBlockOutlineColor[0], orderedWaypointBlockOutlineColor[1], orderedWaypointBlockOutlineColor[2]);
        OrderedWaypointManager.setBlockOutlineAlpha(orderedWaypointBlockOutlineAlpha);
        OrderedWaypointManager.setBlockOutlineThickness(orderedWaypointBlockOutlineThickness);
        OrderedWaypointManager.setBlockOutlineFill(orderedWaypointBlockOutlineFill);
        OrderedWaypointManager.setSkipObstructed(orderedWaypointSkipObstructed);
        OrderedWaypointManager.setObstructedThreshold(orderedWaypointObstructedThreshold);
    }

    public void loadFromGame() {
        commissionHudEnabled = CommissionHUD.isEnabled();
        commissionHudHideWithF1 = CommissionHUD.isHideWithF1();
        commissionHudX = CommissionHUD.getX();
        commissionHudY = CommissionHUD.getY();
        hudAnchorVersion = HUD_ANCHOR_VERSION;
        commissionHudAnchorModeX = CommissionHUD.anchor().modeX();
        commissionHudAnchorOffX = CommissionHUD.anchor().offX();
        commissionHudAnchorModeY = CommissionHUD.anchor().modeY();
        commissionHudAnchorOffY = CommissionHUD.anchor().offY();
        commissionHudScale = CommissionHUD.getScale();
        commissionHudBackground = CommissionHUD.isBackgroundEnabled();
        commissionHudLayout = CommissionHUD.getLayoutMode().name();
        commissionHudStyle = CommissionHUD.getDisplayStyle().name();
        commissionHudShowHeader = CommissionHUD.isShowHeader();
        commissionHudProgressColors = CommissionHUD.isProgressColors();
        commissionHeaderColor = CommissionHUD.getHeaderColor();
        commissionNameColor = CommissionHUD.getNameColor();
        commissionPercentColor = CommissionHUD.getPercentColor();
        commissionDoneColor = CommissionHUD.getDoneColor();
        commissionStatsEnabled = CommTracker.isStatsEnabled();
        commissionShowOverMenus = CommissionHUD.isShowOverMenus();
        commTrackTotal = CommTracker.getTotalCompleted();
        commStatsHudX = CommStatsHUD.getX();
        commStatsHudY = CommStatsHUD.getY();
        commStatsHudAnchorModeX = CommStatsHUD.anchor().modeX();
        commStatsHudAnchorOffX = CommStatsHUD.anchor().offX();
        commStatsHudAnchorModeY = CommStatsHUD.anchor().modeY();
        commStatsHudAnchorOffY = CommStatsHUD.anchor().offY();
        commStatsHudScale = CommStatsHUD.getScale();

        lapisEnabled = CorpseESP.isLapisEnabled();
        tungstenEnabled = CorpseESP.isTungstenEnabled();
        umberEnabled = CorpseESP.isUmberEnabled();
        vanguardEnabled = CorpseESP.isVanguardEnabled();

        shaftESPEnabled = ShaftESP.isLittlefootEnabled();
        shaftESPLittlefootTracer = ShaftESP.isLittlefootTracer();
        shaftESPMobsEnabled = ShaftESP.isMobsEnabled();
        shaftESPMobColor = ShaftESP.getMobColor();
        shaftESPMobAlpha = ShaftESP.getMobAlpha();
        for (forfun.miningqol.client.SkinMob mob : forfun.miningqol.client.SkinMob.values()) {
            if (mob != forfun.miningqol.client.SkinMob.LITTLEFOOT) shaftESPSkinMobEnabled.put(mob.name(), mob.isEnabled());
        }
        shaftESPLittlefootColor = ShaftESP.getLittlefootColor();
        corpseESPRenderMode = CorpseESP.getRenderMode().name();

        blockOverlayEnabled = BlockOverlay.isEnabled();
        blockOverlayMode = BlockOverlay.getMode().name();
        blockOverlayFillColor = BlockOverlay.getFillColor();
        blockOverlayFillAlpha = BlockOverlay.getFillAlpha();
        blockOverlayOutlineColor = BlockOverlay.getOutlineColor();
        blockOverlayOutlineAlpha = BlockOverlay.getOutlineAlpha();
        blockOverlayLineWidth = BlockOverlay.getLineWidth();
        blockOverlayPhase = BlockOverlay.isPhase();
        blockOverlayHideDuringEtherwarp = BlockOverlay.isHideDuringEtherwarp();

        pickaxeCooldownEnabled = PickaxeCooldownHUD.isEnabled();
        pickaxeCooldownHideWithF1 = PickaxeCooldownHUD.isHideWithF1();
        pickaxeCooldownX = PickaxeCooldownHUD.getX();
        pickaxeCooldownY = PickaxeCooldownHUD.getY();
        pickaxeCooldownAnchorModeX = PickaxeCooldownHUD.anchor().modeX();
        pickaxeCooldownAnchorOffX = PickaxeCooldownHUD.anchor().offX();
        pickaxeCooldownAnchorModeY = PickaxeCooldownHUD.anchor().modeY();
        pickaxeCooldownAnchorOffY = PickaxeCooldownHUD.anchor().offY();
        pickaxeCooldownScale = PickaxeCooldownHUD.getScale();
        pickaxeCooldownTitleEnabled = PickaxeCooldownHUD.isTitleEnabled();
        pickaxeCooldownTitleThreshold = PickaxeCooldownHUD.getTitleThreshold();
        pickaxeCooldownCustomEnabled = PickaxeCooldownHUD.isCustomCooldownEnabled();
        pickaxeCooldownCustomSeconds = PickaxeCooldownHUD.getCustomCooldownSeconds();
        pickaxeCooldownLabelColor = PickaxeCooldownHUD.getCooldownLabelColor();
        pickaxeCooldownValueColor = PickaxeCooldownHUD.getCooldownValueColor();
        pickaxeReadyLabelColor = PickaxeCooldownHUD.getReadyLabelColor();
        pickaxeReadyValueColor = PickaxeCooldownHUD.getReadyValueColor();
        pickaxeCooldownOnly = PickaxeCooldownHUD.isCooldownOnly();
        pickaxeTextAlign = PickaxeCooldownHUD.getTextAlign();
        pickaxeSecondsSuffix = PickaxeCooldownHUD.isSecondsSuffix();
        pickaxeActiveTimerEnabled = PickaxeCooldownHUD.isActiveTimerEnabled();
        pickaxeActiveLabelColor = PickaxeCooldownHUD.getActiveLabelColor();
        pickaxeActiveValueColor = PickaxeCooldownHUD.getActiveValueColor();

        filetWarningEnabled = FiletWarning.isEnabled();
        rollingMinerCooldownEnabled = RollingMinerCooldown.isEnabled();
        rogueSwordTimer = forfun.miningqol.client.ItemCooldownTimer.ROGUE_SWORD.save();
        tubaTimer = forfun.miningqol.client.ItemCooldownTimer.TUBA.save();
        rollingMinerCooldownX = RollingMinerCooldown.getX();
        rollingMinerCooldownY = RollingMinerCooldown.getY();
        rollingMinerCooldownAnchorModeX = RollingMinerCooldown.anchor().modeX();
        rollingMinerCooldownAnchorOffX = RollingMinerCooldown.anchor().offX();
        rollingMinerCooldownAnchorModeY = RollingMinerCooldown.anchor().modeY();
        rollingMinerCooldownAnchorOffY = RollingMinerCooldown.anchor().offY();
        rollingCooldownLabelColor = RollingMinerCooldown.getCooldownLabelColor();
        rollingCooldownValueColor = RollingMinerCooldown.getCooldownValueColor();
        rollingReadyLabelColor = RollingMinerCooldown.getReadyLabelColor();
        rollingReadyValueColor = RollingMinerCooldown.getReadyValueColor();

        shaftSummaryEnabled = forfun.miningqol.client.summary.ShaftSummary.isEnabled();
        shaftSummaryFrontLoaded = forfun.miningqol.client.summary.ShaftSummary.isFrontLoaded();
        shaftSummaryTrackAll = forfun.miningqol.client.summary.ShaftSummary.isTrackAllShaftSummaries();
        shaftSummaryShowBuffs = forfun.miningqol.client.summary.ShaftSummary.isShowBuffsInSummary();
        shaftSummaryTrackEfficiency = forfun.miningqol.client.summary.ShaftSummary.isTrackEfficiencyInSummary();
        shaftSummaryEfficiencyPause = forfun.miningqol.client.summary.ShaftSummary.getEfficiencyPauseSeconds();
        shaftSummaryAutoSave = forfun.miningqol.client.summary.ShaftSummary.isAutoSave();
        shaftSummaryPrintToChat = forfun.miningqol.client.summary.ShaftSummary.isPrintToChat();
        shaftSummaryPrintToParty = forfun.miningqol.client.summary.ShaftSummary.isPrintToParty();
        shaftSummaryMaxSavedRuns = forfun.miningqol.client.summary.ShaftSummary.getMaxSavedRuns();
        shaftSummaryTypes = new java.util.ArrayList<>(forfun.miningqol.client.summary.ShaftSummary.getSummaryShaftTypes());
        shaftSummarySavedTypes = new java.util.ArrayList<>(forfun.miningqol.client.summary.ShaftSummary.getSavedShaftTypes());
        shaftSummaryRanksBy = new java.util.ArrayList<>(forfun.miningqol.client.summary.ShaftSummary.getRanksBy());
        shaftSummaryGemMiningSpeed = forfun.miningqol.client.summary.ShaftSummary.getGemMiningSpeed();
        shaftSummaryBlueCheese = forfun.miningqol.client.summary.ShaftSummary.isBlueCheese();
        shaftSummaryValueGemsAsFlawless = forfun.miningqol.client.summary.ShaftSummary.isValueAsFlawless();
        shaftSummarySellOffer = forfun.miningqol.client.summary.ShaftSummary.isSellOffer();

        skyMallBuff = forfun.miningqol.client.summary.SkyMallTracker.storedBuffName();
        skyMallBuffUntil = forfun.miningqol.client.summary.SkyMallTracker.storedUntil();

        wispRadiusEnabled = WispRadius.isEnabled();
        wispRadiusEdgeOnly = WispRadius.isEdgeOnly();
        wispRadiusFloorBlocksOnly = WispRadius.isFloorBlocksOnly();
        wispRadiusRadius = WispRadius.getRadius();
        wispRadiusAlpha = WispRadius.getAlpha();
        wispRadiusColor = WispRadius.getColor().clone();
        customFogEnabled = forfun.miningqol.client.CustomFog.isEnabled();
        customFogStart = forfun.miningqol.client.CustomFog.getStart();
        customFogEnd = forfun.miningqol.client.CustomFog.getEnd();
        customFogColor = forfun.miningqol.client.CustomFog.getColor();
        customFogAlpha = forfun.miningqol.client.CustomFog.getAlpha();
        customFogSky = forfun.miningqol.client.CustomFog.isFogSky();
        mineshaftPortalEnabled = forfun.miningqol.client.MineshaftPortal.isEnabled();
        mineshaftPortalTracer = forfun.miningqol.client.MineshaftPortal.isTracer();
        mineshaftPortalColor = forfun.miningqol.client.MineshaftPortal.getColor();
        mineshaftPortalAlpha = forfun.miningqol.client.MineshaftPortal.getAlpha();
        mineshaftPortalLineWidth = forfun.miningqol.client.MineshaftPortal.getLineWidth();

        mayhemHudEnabled = MayhemHUD.isEnabled();
        mayhemHudHideWithF1 = MayhemHUD.isHideWithF1();
        mayhemHudAlwaysShow = MayhemHUD.isAlwaysShow();
        mayhemHudX = MayhemHUD.getX();
        mayhemHudY = MayhemHUD.getY();
        mayhemHudAnchorModeX = MayhemHUD.anchor().modeX();
        mayhemHudAnchorOffX = MayhemHUD.anchor().offX();
        mayhemHudAnchorModeY = MayhemHUD.anchor().modeY();
        mayhemHudAnchorOffY = MayhemHUD.anchor().offY();
        mayhemLabelColor = MayhemHUD.getLabelColor();
        mayhemValueColor = MayhemHUD.getValueColor();
        mayhemNoneColor = MayhemHUD.getNoneColor();

        commGuiEnabled = forfun.miningqol.client.CommissionGui.isEnabled();
        commGuiShowProgress = forfun.miningqol.client.CommissionGui.isShowProgress();
        commGuiScale = forfun.miningqol.client.CommissionGui.getScale();
        commGuiButtonSize = forfun.miningqol.client.CommissionGui.getButtonSize();
        commGuiAccentColor = forfun.miningqol.client.CommissionGui.getAccentColor();
        commGuiDoneColor = forfun.miningqol.client.CommissionGui.getDoneColor();

        shatterScale = forfun.miningqol.client.shatter.ShatterUi.scale;
        shatterAnimations = forfun.miningqol.client.shatter.ShatterUi.animations();
        shatterDescriptions = forfun.miningqol.client.shatter.ShatterUi.descriptions();
        shatterOpacity = forfun.miningqol.client.shatter.ShatterUi.opacity.get();
        shatterOutline = forfun.miningqol.client.shatter.ShatterUi.outline.get();
        shatterOutlineWidth = forfun.miningqol.client.shatter.ShatterUi.outlineWidth.get();
        shatterPanels = forfun.miningqol.client.shatter.ShatterConfig.store();
        efficientMinerEnabled = EfficientMinerOverlay.isEnabled();
        useOldHeatmap = EfficientMinerOverlay.isUsingOldHeatmap();

        forgeDisplayEnabled = ForgeDisplay.isEnabled();
        forgeDisplayHideWithF1 = ForgeDisplay.isHideWithF1();
        forgeDisplayShowEmpty = ForgeDisplay.isShowEmpty();
        forgeDisplaySortByTime = ForgeDisplay.isSortByTime();
        forgeDisplayX = ForgeDisplay.getX();
        forgeDisplayY = ForgeDisplay.getY();
        forgeDisplayAnchorModeX = ForgeDisplay.anchor().modeX();
        forgeDisplayAnchorOffX = ForgeDisplay.anchor().offX();
        forgeDisplayAnchorModeY = ForgeDisplay.anchor().modeY();
        forgeDisplayAnchorOffY = ForgeDisplay.anchor().offY();
        forgeTitleColor = ForgeDisplay.getTitleColor();
        forgeItemColor = ForgeDisplay.getItemColor();
        forgeTimeColor = ForgeDisplay.getTimeColor();
        forgeReadyColor = ForgeDisplay.getReadyColor();

        autoPartyEnabled = MineshaftAutoParty.isEnabled();
        autoPartyDisbandAfterWarp = MineshaftAutoParty.isDisbandAfterWarp();
        autoPartyDisbandSeconds = MineshaftAutoParty.getDisbandSeconds();
        autoPartyWarpDelaySeconds = MineshaftAutoParty.getWarpDelaySeconds();
        autoPartyDisbandOnTimeout = MineshaftAutoParty.isDisbandOnTimeout();
        autoPartySettleSeconds = MineshaftAutoParty.getSettleSeconds();
        autoPartyAcceptEnabled = PartyAutoAccept.isEnabled();
        autoPartyAcceptBlockAbility = PartyAutoAccept.isBlockDuringAbility();
        autoPartyAcceptBlockInShaft = PartyAutoAccept.isBlockInShaft();
        autoPartyAcceptBlockWhenReady = PartyAutoAccept.isBlockWhenReady();
        autoPartyAcceptList = PartyAutoAccept.names();
        autoPartySignups = MineshaftAutoParty.exportSignups();
        autoPartyCorpseSignups = MineshaftAutoParty.exportCorpseSignups();
        autoPartyLittlefootMob = MineshaftAutoParty.exportMobSignups();
        autoPartyBlockedSignups = MineshaftAutoParty.exportBlockedSignups();
        autoPartyDisabledPlayers = MineshaftAutoParty.exportDisabledPlayers();

        commandKeybinds.clear();
        for (java.util.Map.Entry<Integer, String> entry : CommandKeybindManager.getAllKeybinds().entrySet()) {
            commandKeybinds.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        lobbyFinderBlocks.clear();
        for (net.minecraft.core.BlockPos pos : LobbyFinder.getTrackedBlocks()) {
            lobbyFinderBlocks.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }

        chatLogsEnabled = MqoChat.isLogsEnabled();
        critParticleDrop = CritParticleDrop.isEnabled();

        soundBlockingEnabled = SoundBlocker.isBlockingEnabled();
        soundBlockRules = SoundBlocker.getRules();

        if (forfun.miningqol.client.CheatHooks.storeConfig != null) {
            forfun.miningqol.client.CheatHooks.storeConfig.run();
        }

        orderedWaypointsEnabled = OrderedWaypointManager.isEnabledRaw();
        orderedWaypointRange = OrderedWaypointManager.getWaypointRange();
        orderedWaypointNextCount = OrderedWaypointManager.getNextCount();
        orderedWaypointTraceLine = OrderedWaypointManager.isTraceLineEnabled();
        orderedWaypointShowDistance = OrderedWaypointManager.isShowDistance();
        orderedWaypointShowName = OrderedWaypointManager.isShowName();
        orderedWaypointCurrentColor = OrderedWaypointManager.getCurrentWaypointColor();
        orderedWaypointNextColor = OrderedWaypointManager.getNextWaypointColor();
        orderedWaypointPreviousColor = OrderedWaypointManager.getPreviousWaypointColor();
        orderedWaypointTraceLineColor = OrderedWaypointManager.getTraceLineColor();
        orderedWaypointCurrentAlpha = OrderedWaypointManager.getCurrentWaypointAlpha();
        orderedWaypointNextAlpha = OrderedWaypointManager.getNextWaypointAlpha();
        orderedWaypointPreviousAlpha = OrderedWaypointManager.getPreviousWaypointAlpha();
        orderedWaypointTraceLineAlpha = OrderedWaypointManager.getTraceLineAlpha();
        orderedWaypointLobbyCheckEnabled = OrderedWaypointManager.isLobbyCheckEnabled();
        orderedWaypointLobbyCheckBlock = OrderedWaypointManager.getLobbyCheckBlock();
        orderedWaypointLobbyCheckInterval = OrderedWaypointManager.getLobbyCheckInterval();
        orderedWaypointLobbyCheckRadius = OrderedWaypointManager.getLobbyCheckRadius();
        orderedWaypointBlockOutline = OrderedWaypointManager.isBlockOutlineAroundWaypoint();
        orderedWaypointBlockOutlineRadius = OrderedWaypointManager.getBlockOutlineRadius();
        orderedWaypointBlockOutlineColor = OrderedWaypointManager.getBlockOutlineColor();
        orderedWaypointBlockOutlineAlpha = OrderedWaypointManager.getBlockOutlineAlpha();
        orderedWaypointBlockOutlineThickness = OrderedWaypointManager.getBlockOutlineThickness();
        orderedWaypointBlockOutlineFill = OrderedWaypointManager.isBlockOutlineFill();
        orderedWaypointSkipObstructed = OrderedWaypointManager.isSkipObstructed();
        orderedWaypointObstructedThreshold = OrderedWaypointManager.getObstructedThreshold();
    }
}
