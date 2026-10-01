package forfun.miningqol.client.summary;

/**
 * One finished shaft, as kept in the run history file. Plain fields so Gson can
 * round-trip it; older files missing a field read back as the default.
 */
public class ShaftRun {
    public long timestamp;
    public String shaftTypeId;
    public int totalGemstones;
    public double finesCount;
    public long estimatedProfit;
    public long durationSeconds;
    public int lapisCorpses;
    public int campfiresUsed;
    public boolean mineshaftMayhem;
    public String mayhemPerk;
    public boolean skyMallBuff;
    public String skymallPerk;
    public String mineshaftEvent;
    public boolean frontLoaded;
    /** Null when efficiency tracking was off for the run. */
    public Float efficiencyPercent;

    public ShaftPbType summaryType() {
        ShaftPbType type = ShaftPbType.fromKey(shaftTypeId);
        return type == null ? ShaftPbType.JASPER : type;
    }
}
