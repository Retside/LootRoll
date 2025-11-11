package me.newtale.lootroll.manager;

import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyManager {

    private final ConfigManager configManager;
    private final PartiesAPI partiesAPI;
    private final PartyType partyType;

    public enum PartyType {
        PARTIES,
        MMOCORE
    }

    public PartyManager(ConfigManager configManager) {
        this.configManager = configManager;

        String partyTypeString = configManager.getConfig().getString("party-system", "parties").toLowerCase();

        switch (partyTypeString) {
            case "mmocore":
                if (Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
                    this.partyType = PartyType.MMOCORE;
                    this.partiesAPI = null;
                    break;
                } else {
                    configManager.getPlugin().getLogger().warning("MMOCore not found! Falling back to Parties plugin.");
                }
            case "parties":
            default:
                this.partyType = PartyType.PARTIES;
                this.partiesAPI = getPartiesAPI();
                break;
        }

        configManager.getPlugin().getLogger().info("Using party system: " + this.partyType.name());
    }

    // Backwards compatibility constructor
    public PartyManager(PartiesAPI partiesAPI, ConfigManager configManager) {
        this.partiesAPI = partiesAPI;
        this.configManager = configManager;
        this.partyType = PartyType.PARTIES;
    }

    private PartiesAPI getPartiesAPI() {
        try {
            return com.alessiodp.parties.api.Parties.getApi();
        } catch (Exception e) {
            configManager.getPlugin().getLogger().warning("Failed to get Parties API: " + e.getMessage());
            return null;
        }
    }

    public boolean isPlayerInParty(Player player) {
        switch (partyType) {
            case MMOCORE:
                return isPlayerInMMOCoreParty(player);
            case PARTIES:
            default:
                return isPlayerInPartiesPlugin(player);
        }
    }

    public List<Player> getPartyMembers(Player player) {
        switch (partyType) {
            case MMOCORE:
                return getMMOCorePartyMembers(player);
            case PARTIES:
            default:
                return getPartiesPluginMembers(player);
        }
    }

    public boolean hasMultiplePartyMembers(Player player) {
        List<Player> members = getPartyMembers(player);
        return members.size() > 1;
    }

    private boolean isPlayerInMMOCoreParty(Player player) {
        try {
            PlayerData playerData = PlayerData.get(player);
            if (playerData == null) return false;

            AbstractParty party = playerData.getParty();
            if (party == null) return false;

            return party.countMembers() > 1;
        } catch (Exception e) {
            configManager.getPlugin().getLogger().warning("Error checking MMOCore party for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private List<Player> getMMOCorePartyMembers(Player player) {
        List<Player> members = new ArrayList<>();

        try {
            PlayerData playerData = PlayerData.get(player);
            if (playerData == null) return members;

            AbstractParty party = playerData.getParty();
            if (party == null) return members;

            double maxDistance = configManager.getRollDistance();

            for (PlayerData memberData : party.getOnlineMembers()) {
                Player member = memberData.getPlayer();
                if (member != null && member.isOnline()) {
                    if (member.getLocation().distance(player.getLocation()) <= maxDistance) {
                        members.add(member);
                    }
                }
            }
        } catch (Exception e) {
            configManager.getPlugin().getLogger().warning("Error getting MMOCore party members for " + player.getName() + ": " + e.getMessage());
        }

        return members;
    }

    private boolean isPlayerInPartiesPlugin(Player player) {
        if (partiesAPI == null) return false;

        try {
            PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
            if (partyPlayer == null) return false;

            Party party = partiesAPI.getParty(partyPlayer.getPartyId());
            return party != null && party.getMembers().size() > 1;
        } catch (Exception e) {
            configManager.getPlugin().getLogger().warning("Error checking Parties plugin party for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private List<Player> getPartiesPluginMembers(Player player) {
        List<Player> members = new ArrayList<>();

        if (partiesAPI == null) return members;

        try {
            PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
            if (partyPlayer == null) return members;

            Party party = partiesAPI.getParty(partyPlayer.getPartyId());
            if (party == null) return members;

            double maxDistance = configManager.getRollDistance();

            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    if (member.getLocation().distance(player.getLocation()) <= maxDistance) {
                        members.add(member);
                    }
                }
            }
        } catch (Exception e) {
            configManager.getPlugin().getLogger().warning("Error getting Parties plugin members for " + player.getName() + ": " + e.getMessage());
        }

        return members;
    }

    public PartyType getPartyType() {
        return partyType;
    }
}