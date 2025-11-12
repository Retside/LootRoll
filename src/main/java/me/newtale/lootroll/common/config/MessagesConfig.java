package me.newtale.lootroll.common.config;

public class MessagesConfig {
    
    private String reloadSuccess = "Configuration successfully reloaded!";
    private String lootDropped = "<newline> <green>Item dropped</green> <gray>[</gray><item><gray>]</gray><newline> <gray><hover:show_text:'<gray>Click to make priority roll.'><click:run_command:/roll <item_name>><white>Need</click></hover> | <hover:show_text:'<gray>Click to make a non-priority roll.'><click:run_command:/greed <item_name>><yellow>Greed</click></hover> | <hover:show_text:'<gray>Click to pass the item.'><click:run_command:/pass <item_name>><red>Pass</click></hover><newline>";
    private String countdown = "<gray>Roll for item [</gray><item><gray>]</gray> will end in <time> seconds!";
    private String playerRolled = "<yellow>Player <player> rolls <gold><roll></gold> for</yellow> <gray>[</gray><item><gray>]</gray>";
    private String playerGreedRolled = "<yellow>Player <player> greed rolls <gray><roll></gray> for</yellow> <gray>[</gray><item><gray>]</gray>";
    private String playerSkippedRoll = "<yellow>Player <player> passes on</yellow> <gray>[</gray><item><gray>]</gray>";
    private String rollWinner = "<green>Player <player> won</green> <gray>[</gray><item><gray>]</gray> <green>with roll <gold><roll></gold>";
    private String greedRollWinner = "<green>Player <player> won</green> <gray>[</gray><item><gray>]</gray> <green>with greed roll <gray><roll></gray>";
    private String availableItems = "<gray>Active rolls:";
    private String availableItemFormat = " <dark_gray>- <click:run_command:/roll <item_name>><gray>[</gray><item><gray>]</gray></click>";
    private String availableItemsToSkip = "<gray>Active rolls:";
    private String availableSkipItemFormat = " <dark_gray>- <click:run_command:/pass <item_name>><gray>[</gray><item><gray>]</gray></click>";
    private String noActiveRoll = "<red>No active rolls!";
    private String noActiveGreedRoll = "<red>No active rolls!";
    private String noSessionsToSkip = "<red>No active rolls!";
    private String notInParty = "<red>You must be in a party to use the roll command!";
    private String noRollsUnlocked = "<gray>Nobody rolled for <gray>[</gray><item><gray>]</gray>!";
    private String itemNotFound = "<red>Item '<item>' not found!";
    private String expPlaceholder = "<green><amount> EXP</green>";
    private String moneyPlaceholder = "<gold><amount></gold>";
    
    public MessagesConfig() {
        // Default constructor
    }
    
    // Getters and setters
    public String getReloadSuccess() {
        return reloadSuccess;
    }
    
    public void setReloadSuccess(String reloadSuccess) {
        this.reloadSuccess = reloadSuccess;
    }
    
    public String getLootDropped() {
        return lootDropped;
    }
    
    public void setLootDropped(String lootDropped) {
        this.lootDropped = lootDropped;
    }
    
    public String getCountdown() {
        return countdown;
    }
    
    public void setCountdown(String countdown) {
        this.countdown = countdown;
    }
    
    public String getPlayerRolled() {
        return playerRolled;
    }
    
    public void setPlayerRolled(String playerRolled) {
        this.playerRolled = playerRolled;
    }
    
    public String getPlayerGreedRolled() {
        return playerGreedRolled;
    }
    
    public void setPlayerGreedRolled(String playerGreedRolled) {
        this.playerGreedRolled = playerGreedRolled;
    }
    
    public String getPlayerSkippedRoll() {
        return playerSkippedRoll;
    }
    
    public void setPlayerSkippedRoll(String playerSkippedRoll) {
        this.playerSkippedRoll = playerSkippedRoll;
    }
    
    public String getRollWinner() {
        return rollWinner;
    }
    
    public void setRollWinner(String rollWinner) {
        this.rollWinner = rollWinner;
    }
    
    public String getGreedRollWinner() {
        return greedRollWinner;
    }
    
    public void setGreedRollWinner(String greedRollWinner) {
        this.greedRollWinner = greedRollWinner;
    }
    
    public String getAvailableItems() {
        return availableItems;
    }
    
    public void setAvailableItems(String availableItems) {
        this.availableItems = availableItems;
    }
    
    public String getAvailableItemFormat() {
        return availableItemFormat;
    }
    
    public void setAvailableItemFormat(String availableItemFormat) {
        this.availableItemFormat = availableItemFormat;
    }
    
    public String getAvailableItemsToSkip() {
        return availableItemsToSkip;
    }
    
    public void setAvailableItemsToSkip(String availableItemsToSkip) {
        this.availableItemsToSkip = availableItemsToSkip;
    }
    
    public String getAvailableSkipItemFormat() {
        return availableSkipItemFormat;
    }
    
    public void setAvailableSkipItemFormat(String availableSkipItemFormat) {
        this.availableSkipItemFormat = availableSkipItemFormat;
    }
    
    public String getNoActiveRoll() {
        return noActiveRoll;
    }
    
    public void setNoActiveRoll(String noActiveRoll) {
        this.noActiveRoll = noActiveRoll;
    }
    
    public String getNoActiveGreedRoll() {
        return noActiveGreedRoll;
    }
    
    public void setNoActiveGreedRoll(String noActiveGreedRoll) {
        this.noActiveGreedRoll = noActiveGreedRoll;
    }
    
    public String getNoSessionsToSkip() {
        return noSessionsToSkip;
    }
    
    public void setNoSessionsToSkip(String noSessionsToSkip) {
        this.noSessionsToSkip = noSessionsToSkip;
    }
    
    public String getNotInParty() {
        return notInParty;
    }
    
    public void setNotInParty(String notInParty) {
        this.notInParty = notInParty;
    }
    
    public String getNoRollsUnlocked() {
        return noRollsUnlocked;
    }
    
    public void setNoRollsUnlocked(String noRollsUnlocked) {
        this.noRollsUnlocked = noRollsUnlocked;
    }
    
    public String getItemNotFound() {
        return itemNotFound;
    }
    
    public void setItemNotFound(String itemNotFound) {
        this.itemNotFound = itemNotFound;
    }
    
    public String getExpPlaceholder() {
        return expPlaceholder;
    }
    
    public void setExpPlaceholder(String expPlaceholder) {
        this.expPlaceholder = expPlaceholder;
    }
    
    public String getMoneyPlaceholder() {
        return moneyPlaceholder;
    }
    
    public void setMoneyPlaceholder(String moneyPlaceholder) {
        this.moneyPlaceholder = moneyPlaceholder;
    }
}

