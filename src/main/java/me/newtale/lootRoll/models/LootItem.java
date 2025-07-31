package me.newtale.lootRoll.models;

public class LootItem {
    private final String type;
    private final String itemId;
    private final double chance;
    private final int amount;
    private final boolean unidentified;
    private final int minDrops;
    private final int maxDrops;

    public LootItem(String type, String itemId, double chance, int amount) {
        this(type, itemId, chance, amount, false, 1, 1);
    }

    public LootItem(String type, String itemId, double chance, int amount, boolean unidentified) {
        this(type, itemId, chance, amount, unidentified, 1, 1);
    }

    public LootItem(String type, String itemId, double chance, int amount, boolean unidentified, int minDrops, int maxDrops) {
        this.type = type;
        this.itemId = itemId;
        this.chance = chance;
        this.amount = amount;
        this.unidentified = unidentified;
        this.minDrops = Math.max(1, minDrops);
        this.maxDrops = Math.max(this.minDrops, maxDrops);
    }

    public String getType() {
        return type;
    }

    public String getItemId() {
        return itemId;
    }

    public double getChance() {
        return chance;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isUnidentified() {
        return unidentified;
    }

    public int getMinDrops() {
        return minDrops;
    }

    public int getMaxDrops() {
        return maxDrops;
    }

    @Override
    public String toString() {
        return "LootItem{" +
                "type='" + type + '\'' +
                ", itemId='" + itemId + '\'' +
                ", chance=" + chance +
                ", amount=" + amount +
                ", unidentified=" + unidentified +
                ", minDrops=" + minDrops +
                ", maxDrops=" + maxDrops +
                '}';
    }
}