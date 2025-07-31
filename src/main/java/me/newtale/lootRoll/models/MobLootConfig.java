package me.newtale.lootRoll.models;

import java.util.List;

public class MobLootConfig {
    private final String mobId;
    private final List<LootItem> lootItems;
    private final int minDrops;
    private final int maxDrops;

    public MobLootConfig(String mobId, List<LootItem> lootItems, int minDrops, int maxDrops) {
        this.mobId = mobId;
        this.lootItems = lootItems;
        this.minDrops = Math.max(0, minDrops);
        this.maxDrops = Math.max(this.minDrops, maxDrops);
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

    @Override
    public String toString() {
        return "MobLootConfig{" +
                "mobId='" + mobId + '\'' +
                ", lootItems=" + lootItems.size() +
                ", minDrops=" + minDrops +
                ", maxDrops=" + maxDrops +
                '}';
    }
}