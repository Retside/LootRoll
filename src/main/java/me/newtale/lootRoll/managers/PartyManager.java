package me.newtale.lootRoll.managers;

import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyManager {

    private final PartiesAPI partiesAPI;
    private final ConfigManager configManager;

    public PartyManager(PartiesAPI partiesAPI, ConfigManager configManager) {
        this.partiesAPI = partiesAPI;
        this.configManager = configManager;
    }

    public boolean isPlayerInParty(Player player) {
        if (partiesAPI == null) return false;

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null) return false;

        Party party = partiesAPI.getParty(partyPlayer.getPartyId());
        return party != null && party.getMembers().size() > 1;
    }

    public List<Player> getPartyMembers(Player player) {
        List<Player> members = new ArrayList<>();

        if (partiesAPI == null) return members;

        PartyPlayer partyPlayer = partiesAPI.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null) return members;

        Party party = partiesAPI.getParty(partyPlayer.getPartyId());
        if (party == null) return members;

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                double maxDistance = configManager.getRollDistance();
                if (member.getLocation().distance(player.getLocation()) <= maxDistance) {
                    members.add(member);
                }
            }
        }

        return members;
    }

    public boolean hasMultiplePartyMembers(Player player) {
        List<Player> members = getPartyMembers(player);
        return members.size() > 1;
    }
}