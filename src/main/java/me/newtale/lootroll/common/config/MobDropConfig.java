package me.newtale.lootroll.common.config;

import java.util.ArrayList;
import java.util.List;

public class MobDropConfig {
    
    private int minDrops = 0;
    private int maxDrops = 0;
    private boolean overrideVanillaDrops = false;
    private boolean processVanillaDrops = false;
    
    private List<String> loot = new ArrayList<>();
    
    public MobDropConfig() {
        // Default constructor
    }
    
    public int getMinDrops() {
        return minDrops;
    }
    
    public void setMinDrops(int minDrops) {
        this.minDrops = minDrops;
    }
    
    public int getMaxDrops() {
        return maxDrops;
    }
    
    public void setMaxDrops(int maxDrops) {
        this.maxDrops = maxDrops;
    }
    
    public boolean isOverrideVanillaDrops() {
        return overrideVanillaDrops;
    }
    
    public void setOverrideVanillaDrops(boolean overrideVanillaDrops) {
        this.overrideVanillaDrops = overrideVanillaDrops;
    }
    
    public boolean isProcessVanillaDrops() {
        return processVanillaDrops;
    }
    
    public void setProcessVanillaDrops(boolean processVanillaDrops) {
        this.processVanillaDrops = processVanillaDrops;
    }
    
    public List<String> getLoot() {
        return loot;
    }
    
    public void setLoot(List<String> loot) {
        this.loot = loot != null ? loot : new ArrayList<>();
    }
}

