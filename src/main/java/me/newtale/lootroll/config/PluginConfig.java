package me.newtale.lootroll.config;

public class PluginConfig {
    
    private int configVersion = 1;
    private String partySystem = "parties";
    private double rollDistance = 100.0;
    private int rollTime = 30;
    
    private MessagesConfig messages = new MessagesConfig();
    
    public PluginConfig() {
        // Default constructor
    }
    
    public int getConfigVersion() {
        return configVersion;
    }
    
    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }
    
    public String getPartySystem() {
        return partySystem;
    }
    
    public void setPartySystem(String partySystem) {
        this.partySystem = partySystem;
    }
    
    public double getRollDistance() {
        return rollDistance;
    }
    
    public void setRollDistance(double rollDistance) {
        this.rollDistance = rollDistance;
    }
    
    public int getRollTime() {
        return rollTime;
    }
    
    public void setRollTime(int rollTime) {
        this.rollTime = rollTime;
    }
    
    public MessagesConfig getMessages() {
        return messages;
    }
    
    public void setMessages(MessagesConfig messages) {
        this.messages = messages;
    }
}

