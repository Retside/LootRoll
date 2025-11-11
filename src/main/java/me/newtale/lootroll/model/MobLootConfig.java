package me.newtale.lootroll.model;

import java.util.List;

public class MobLootConfig {
    private final String mobId;
    private final List<LootItem> lootItems;
    private final int minDrops;
    private final int maxDrops;
    private final boolean overrideVanillaDrops;
    private final boolean processVanillaDrops;

    public MobLootConfig(String mobId, List<LootItem> lootItems, int minDrops, int maxDrops) {
        this(mobId, lootItems, minDrops, maxDrops, false, false);
    }

    public MobLootConfig(String mobId, List<LootItem> lootItems, int minDrops, int maxDrops, 
                        boolean overrideVanillaDrops, boolean processVanillaDrops) {
        this.mobId = mobId;
        this.lootItems = lootItems;
        this.minDrops = Math.max(0, minDrops);
        this.maxDrops = Math.max(this.minDrops, maxDrops);
        this.overrideVanillaDrops = overrideVanillaDrops;
        this.processVanillaDrops = processVanillaDrops;
    }

    public String getMobId() {
        return mobId;
    }

    public List<LootItem> getLootItems() {
        return lootItems;
    }

    public int getMinDrops() {
        return minDrops;
    }

    public int getMaxDrops() {
        return maxDrops;
    }

    public boolean shouldOverrideVanillaDrops() {
        return overrideVanillaDrops;
    }

    public boolean shouldProcessVanillaDrops() {
        return processVanillaDrops;
    }

    @Override
    public String toString() {
        return "MobLootConfig{" +
                "mobId='" + mobId + '\'' +
                ", lootItems=" + lootItems.size() +
                ", minDrops=" + minDrops +
                ", maxDrops=" + maxDrops +
                ", overrideVanillaDrops=" + overrideVanillaDrops +
                ", processVanillaDrops=" + processVanillaDrops +
                '}';
    }
}