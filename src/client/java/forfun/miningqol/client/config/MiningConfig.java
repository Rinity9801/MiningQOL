package forfun.miningqol.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forfun.miningqol.client.BlockOutlineRenderer;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.NameHider;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.profit.BazaarPriceManager;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MiningConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/miningqol.json");

    public boolean lapisEnabled = true;
    public boolean tungstenEnabled = true;
    public boolean umberEnabled = true;
    public boolean vanguardEnabled = true;

    public boolean profitTrackerEnabled = false;
    public int profitTrackerX = 10;
    public int profitTrackerY = 10;
    public int pristineChance = 20;
    public boolean includeRough = false;
    public boolean useNPCPrices = false;
    public int gemTier = 1;

    public boolean efficientMinerEnabled = false;
    public boolean useOldHeatmap = false;

    public boolean blockOutlineEnabled = false;
    public String blockOutlineMode = "BOTH";
    public float blockOutlineRed = 0.0f;
    public float blockOutlineGreen = 0.0f;
    public float blockOutlineBlue = 0.0f;
    public float blockOutlineAlpha = 0.4f;

    public boolean pickaxeCooldownEnabled = true;
    public int pickaxeCooldownX = 10;
    public int pickaxeCooldownY = 50;
    public boolean pickaxeCooldownTitleEnabled = true;
    public int pickaxeCooldownTitleThreshold = 5;

    public boolean nameHiderEnabled = false;
    public String replacementName = "Player";
    public boolean useGradient = false;
    public float nameColorRed1 = 1.0f;
    public float nameColorGreen1 = 1.0f;
    public float nameColorBlue1 = 1.0f;
    public float nameColorRed2 = 1.0f;
    public float nameColorGreen2 = 1.0f;
    public float nameColorBlue2 = 1.0f;

    public static MiningConfig load() {
        if (!CONFIG_FILE.exists()) {
            LOGGER.info("[MiningConfig] Config file not found, creating default");
            MiningConfig config = new MiningConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            MiningConfig config = GSON.fromJson(reader, MiningConfig.class);
            LOGGER.info("[MiningConfig] Config loaded successfully");
            return config;
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to load config: " + e.getMessage());
            return new MiningConfig();
        }
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
                LOGGER.info("[MiningConfig] Config saved successfully");
            }
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to save config: " + e.getMessage());
        }
    }

    public void applyToGame() {
        if (lapisEnabled != CorpseESP.isLapisEnabled()) {
            CorpseESP.toggleLapis();
        }
        if (tungstenEnabled != CorpseESP.isTungstenEnabled()) {
            CorpseESP.toggleTungsten();
        }
        if (umberEnabled != CorpseESP.isUmberEnabled()) {
            CorpseESP.toggleUmber();
        }
        if (vanguardEnabled != CorpseESP.isVanguardEnabled()) {
            CorpseESP.toggleVanguard();
        }

        ProfitTrackerHUD.setEnabled(profitTrackerEnabled);
        ProfitTrackerHUD.setPosition(profitTrackerX, profitTrackerY);
        GemstoneTracker.setPristineChance(pristineChance);
        GemstoneTracker.setIncludeRough(includeRough);
        GemstoneTracker.setGemTier(gemTier);
        BazaarPriceManager.setUseNPCPrices(useNPCPrices);

        EfficientMinerOverlay.setEnabled(efficientMinerEnabled);
        EfficientMinerOverlay.setUseOldHeatmap(useOldHeatmap);

        PickaxeCooldownHUD.setEnabled(pickaxeCooldownEnabled);
        PickaxeCooldownHUD.setPosition(pickaxeCooldownX, pickaxeCooldownY);
        PickaxeCooldownHUD.setTitleEnabled(pickaxeCooldownTitleEnabled);
        PickaxeCooldownHUD.setTitleThreshold(pickaxeCooldownTitleThreshold);

        BlockOutlineRenderer.setEnabled(blockOutlineEnabled);
        try {
            BlockOutlineRenderer.setMode(BlockOutlineRenderer.OutlineMode.valueOf(blockOutlineMode));
        } catch (Exception e) {
            BlockOutlineRenderer.setMode(BlockOutlineRenderer.OutlineMode.BOTH);
        }
        BlockOutlineRenderer.setColor(blockOutlineRed, blockOutlineGreen, blockOutlineBlue, blockOutlineAlpha);

        NameHider.setEnabled(nameHiderEnabled);
        NameHider.setReplacementName(replacementName);
        NameHider.setUseGradient(useGradient);
        NameHider.setColor1(nameColorRed1, nameColorGreen1, nameColorBlue1);
        NameHider.setColor2(nameColorRed2, nameColorGreen2, nameColorBlue2);
    }

    public void loadFromGame() {
        lapisEnabled = CorpseESP.isLapisEnabled();
        tungstenEnabled = CorpseESP.isTungstenEnabled();
        umberEnabled = CorpseESP.isUmberEnabled();
        vanguardEnabled = CorpseESP.isVanguardEnabled();

        profitTrackerEnabled = ProfitTrackerHUD.isEnabled();
        profitTrackerX = ProfitTrackerHUD.getX();
        profitTrackerY = ProfitTrackerHUD.getY();
        pristineChance = GemstoneTracker.getPristineChance();
        includeRough = GemstoneTracker.isIncludingRough();
        gemTier = GemstoneTracker.getGemTier();
        useNPCPrices = BazaarPriceManager.isUsingNPCPrices();

        efficientMinerEnabled = EfficientMinerOverlay.isEnabled();
        useOldHeatmap = EfficientMinerOverlay.isUsingOldHeatmap();

        pickaxeCooldownEnabled = PickaxeCooldownHUD.isEnabled();
        pickaxeCooldownX = PickaxeCooldownHUD.getX();
        pickaxeCooldownY = PickaxeCooldownHUD.getY();
        pickaxeCooldownTitleEnabled = PickaxeCooldownHUD.isTitleEnabled();
        pickaxeCooldownTitleThreshold = PickaxeCooldownHUD.getTitleThreshold();

        blockOutlineEnabled = BlockOutlineRenderer.isEnabled();
        blockOutlineMode = BlockOutlineRenderer.getMode().name();
        blockOutlineRed = BlockOutlineRenderer.getRed();
        blockOutlineGreen = BlockOutlineRenderer.getGreen();
        blockOutlineBlue = BlockOutlineRenderer.getBlue();
        blockOutlineAlpha = BlockOutlineRenderer.getAlpha();

        nameHiderEnabled = NameHider.isEnabled();
        replacementName = NameHider.getReplacementName();
        useGradient = NameHider.isUsingGradient();
        nameColorRed1 = NameHider.getRed1();
        nameColorGreen1 = NameHider.getGreen1();
        nameColorBlue1 = NameHider.getBlue1();
        nameColorRed2 = NameHider.getRed2();
        nameColorGreen2 = NameHider.getGreen2();
        nameColorBlue2 = NameHider.getBlue2();
    }
}
