package forfun.miningqol.client.summary;

import forfun.miningqol.client.party.ShaftType;

import java.util.List;

/**
 * The gemstone families a shaft summary is printed for, mapped from the scoreboard
 * shaft ids. Non-gem shafts (Tungsten, Umber, Titanium, Vanguard, Littlefoot) have no
 * entry and are not summarised.
 */
public enum ShaftPbType {
    JADE("jade", "Jade", "Jade Shaft Summary", "§a", "jade shaft", ShaftType.JADE_1, ShaftType.JADE_2),
    AMBER("amber", "Amber", "Amber Shaft Summary", "§6", "amber shaft", ShaftType.AMBER_1, ShaftType.AMBER_2),
    AMETHYST("amethyst", "Amethyst", "Amethyst Shaft Summary", "§5", "amethyst shaft", ShaftType.AMETHYST_1, ShaftType.AMETHYST_2),
    TOPAZ("topaz", "Topaz", "Topaz Shaft Summary", "§e", "topaz shaft", ShaftType.TOPAZ_1, ShaftType.TOPAZ_2),
    RUBY("ruby", "Ruby", "Ruby Shaft Summary", "§4", "ruby shaft", ShaftType.RUBY_1, ShaftType.RUBY_2),
    SAPPHIRE("sapphire", "Sapphire", "Sapphire Shaft Summary", "§9", "sapphire shaft", ShaftType.SAPPHIRE_1, ShaftType.SAPPHIRE_2),
    PERIDOT("peridot", "Peridot", "Peridot Shaft Summary", "§2", "peridot shaft", ShaftType.PERIDOT_1, ShaftType.PERIDOT_2),
    AQUAMARINE("aquamarine", "Aquamarine", "Aquamarine Shaft Summary", "§3", "aquamarine shaft", ShaftType.AQUAMARINE_1, ShaftType.AQUAMARINE_2),
    CITRINE("citrine", "Citrine", "Citrine Shaft Summary", "§6", "citrine shaft", ShaftType.CITRINE_1, ShaftType.CITRINE_2),
    ONYX("onyx", "Onyx", "Onyx Shaft Summary", "§0", "onyx shaft", ShaftType.ONYX_1, ShaftType.ONYX_2),
    OPAL("opal", "Opal", "Opal Shaft Summary", "§f", "opal shaft", ShaftType.OPAL),
    JASPER("jasper", "Jasper", "Jasper Shaft Summary", "§d", "jasper shaft", ShaftType.JASPER),
    JASPER_CRYSTAL("jasper_crystal", "Jasper Crystal", "Jasper Crystal Summary", "§d", "jasper crystal shaft", ShaftType.JASPER_CRYSTAL),
    OPAL_CRYSTAL("opal_crystal", "Opal Crystal", "Opal Crystal Summary", "§f", "opal crystal shaft", ShaftType.OPAL_CRYSTAL),
    AQUAMARINE_CRYSTAL("aquamarine_crystal", "Aquamarine Crystal", "Aquamarine Crystal Summary", "§3", "aquamarine crystal shaft", ShaftType.AQUAMARINE_CRYSTAL),
    PERIDOT_CRYSTAL("peridot_crystal", "Peridot Crystal", "Peridot Crystal Summary", "§2", "peridot crystal shaft", ShaftType.PERIDOT_CRYSTAL),
    CITRINE_CRYSTAL("citrine_crystal", "Citrine Crystal", "Citrine Crystal Summary", "§6", "citrine crystal shaft", ShaftType.CITRINE_CRYSTAL),
    ONYX_CRYSTAL("onyx_crystal", "Onyx Crystal", "Onyx Crystal Summary", "§0", "onyx crystal shaft", ShaftType.ONYX_CRYSTAL),
    RUBY_CRYSTAL("ruby_crystal", "Ruby Crystal", "Ruby Crystal Summary", "§4", "ruby crystal shaft", ShaftType.RUBY_CRYSTAL);

    private final String key;
    private final String label;
    private final String summaryTitle;
    private final String colorCode;
    private final String partyLabel;
    private final List<ShaftType> shaftTypes;

    ShaftPbType(String key, String label, String summaryTitle, String colorCode, String partyLabel, ShaftType... shaftTypes) {
        this.key = key;
        this.label = label;
        this.summaryTitle = summaryTitle;
        this.colorCode = colorCode;
        this.partyLabel = partyLabel;
        this.shaftTypes = List.of(shaftTypes);
    }

    /** Stable id written into the run history file. */
    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String summaryTitle() {
        return summaryTitle;
    }

    public String colorCode() {
        return colorCode;
    }

    public String partyLabel() {
        return partyLabel;
    }

    /** The gemstone whose fines this shaft's summary counts ("Jasper Crystal" -> "Jasper"). */
    public String trackedGem() {
        int idx = label.indexOf(" Crystal");
        return idx < 0 ? label : label.substring(0, idx);
    }

    public static ShaftPbType fromShaftType(ShaftType type) {
        if (type == null) return null;
        for (ShaftPbType pb : values()) {
            if (pb.shaftTypes.contains(type)) return pb;
        }
        return null;
    }

    public static ShaftPbType fromKey(String key) {
        if (key == null) return null;
        for (ShaftPbType pb : values()) {
            if (pb.key.equalsIgnoreCase(key)) return pb;
        }
        return null;
    }
}
