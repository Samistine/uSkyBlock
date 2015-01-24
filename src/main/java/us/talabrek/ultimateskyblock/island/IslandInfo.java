package us.talabrek.ultimateskyblock.island;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.VaultHandler;
import us.talabrek.ultimateskyblock.WorldGuardHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Data object for an island
 */
public class IslandInfo {
    private static File directory = new File(".");

    private final File file;
    private final FileConfiguration config;
    private final String name;

    public IslandInfo(String islandName) {
        config = new YamlConfiguration();
        file = new File(directory, islandName + ".yml");
        name = islandName;
        if (file.exists()) {
            uSkyBlock.readConfig(config, file);
        }
    }

    public static void setDirectory(File dir) {
        directory = dir;
    }

    public void clearIslandConfig(final String leader) {
        config.set("general.level", 0);
        config.set("general.warpLocationX", 0);
        config.set("general.warpLocationY", 0);
        config.set("general.warpLocationZ", 0);
        config.set("general.warpActive", false);
        config.set("log.logPos", 1);
        config.set("log.1", "\u00a7d[skyblock] The island has been created.");
        setupPartyLeader(leader);
    }

    public void setupPartyLeader(final String leader) {
        config.set("party.leader", leader);
        ConfigurationSection section = config.createSection("party.members." + leader);
        section.set("canChangeBiome", true);
        section.set("canToggleLock", true);
        section.set("canChangeWarp", true);
        section.set("canToggleWarp", true);
        section.set("canInviteOthers", true);
        section.set("canKickOthers", true);
        config.set("party.currentSize", getMembers().size());
        save();
    }

    public void setupPartyMember(final String member) {
        if (!getMembers().contains(member)) {
            config.set("party.currentSize", config.getInt("party.currentSize") + 1);
        }
        ConfigurationSection section = config.createSection("party.members." + member);
        section.set("canChangeBiome", false);
        section.set("canToggleLock", false);
        section.set("canChangeWarp", false);
        section.set("canToggleWarp", false);
        section.set("canInviteOthers", false);
        section.set("canKickOthers", false);
        section.set("canBanOthers", false);
        WorldGuardHandler.addPlayerToOldRegion(config.getString("party.leader"), member);
        save();
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            uSkyBlock.log(Level.SEVERE, "Unable to save island " + file, e);
        }
    }

    public void reload() {
        final InputStream defConfigStream = getClass().getClassLoader().getResourceAsStream("island.yml");
        if (defConfigStream != null) {
            try {
                config.load(new InputStreamReader(defConfigStream, "UTF-8"));
            } catch (IOException | InvalidConfigurationException e) {
                uSkyBlock.log(Level.SEVERE, "Unable to read island-defaults", e);
            }
        }
        save();
    }

    public void updatePartyNumber(final Player player) {
        if (config.getInt("party.maxSize") < 8 && VaultHandler.checkPerk(player.getName(), "usb.extra.partysize", player.getWorld())) {
            config.set("party.maxSize", 8);
        } else if (config.getInt("party.maxSize") < 7 && VaultHandler.checkPerk(player.getName(), "usb.extra.party3", player.getWorld())) {
            config.set("party.maxSize", 7);
        } else if (config.getInt("party.maxSize") < 6 && VaultHandler.checkPerk(player.getName(), "usb.extra.party2", player.getWorld())) {
            config.set("party.maxSize", 6);
        } else if (config.getInt("party.maxSize") < 5 && VaultHandler.checkPerk(player.getName(), "usb.extra.party1", player.getWorld())) {
            config.set("party.maxSize", 5);
        }
        save();
    }

    public String getLeader() {
        return config.getString("party.leader");
    }

    public boolean hasPerm(Player player, String perm) {
        return hasPerm(player.getName(), perm);
    }

    // TODO: 19/12/2014 - R4zorax: UUID
    public boolean hasPerm(String name, String perm) {
        return name.equalsIgnoreCase(getLeader()) || config.getBoolean("party.members." + name + "." + perm);
    }

    public void setBiome(String biome) {
        config.set("general.biome", biome.toUpperCase());
        save();
    }

    public void setWarpLocation(Location loc) {
        config.set("general.warpLocationX", loc.getBlockX());
        config.set("general.warpLocationY", loc.getBlockY());
        config.set("general.warpLocationZ", loc.getBlockZ());
        config.set("general.warpActive", true);
        save();
    }

    public void togglePerm(final String playername, final String perm) {
        if (!config.contains("party.members." + playername + "." + perm)) {
            return;
        }
        if (config.getBoolean("party.members." + playername + "." + perm)) {
            config.set("party.members." + playername + "." + perm, false);
        } else {
            config.set("party.members." + playername + "." + perm, true);
        }
        save();
    }

    public Set<String> getMembers() {
        return config.getConfigurationSection("party.members").getKeys(false);
    }

    public String getBiome() {
        return config.getString("general.biome").toUpperCase();
    }

    public void log(String message) {
        int currentLogPos = config.getInt("log.logPos");
        config.set("log." + (++currentLogPos), message);
        if (currentLogPos <= 10) {
            config.set("log.logPos", currentLogPos);
        } else {
            // Wrap around
            config.set("log.logPos", 1);
        }
    }

    public int getPartySize() {
        return config.getInt("party.currentSize", 1);
    }

    public boolean isLeader(Player player) {
        return isLeader(player.getName());
    }

    public boolean isLeader(String playerName) {
        return getLeader().equalsIgnoreCase(playerName);
    }

    public boolean hasWarp() {
        return config.getBoolean("general.warpActive");
    }

    public boolean isLocked() {
        return config.getBoolean("general.locked");
    }

    public void setWarpActive(boolean active) {
        config.set("general.warpActive", active);
        save();
    }

    public void lock(Player player) {
        WorldGuardHandler.islandLock(player, getLeader());
        config.set("general.locked", true);
        sendMessageToIslandGroup(player.getName() + " locked the island.");
        if (hasWarp()) {
            config.set("general.warpActive", false);
            player.sendMessage(ChatColor.RED + "Since your island is locked, your incoming warp has been deactivated.");
            sendMessageToIslandGroup(player.getName() + " deactivated the island warp.");
        }
        save();
    }

    public void unlock(Player player) {
        WorldGuardHandler.islandUnlock(player, getLeader());
        config.set("general.locked", false);
        sendMessageToIslandGroup(player.getName() + " unlocked the island.");
        save();
    }

    public void sendMessageToIslandGroup(String message) {
        Date date = new Date();
        final String dateTxt = DateFormat.getDateInstance(3).format(date).toString();
        for (String player : getMembers()) {
            if (Bukkit.getPlayer(player) != null) {
                Bukkit.getPlayer(player).sendMessage("\u00a7d[skyblock] " + message);
            }
        }
        log("\u00a7d[" + dateTxt + "] " + message);
    }

    public int getMaxPartySize() {
        return config.getInt("party.maxSize", 4);
    }

    public boolean isBanned(Player player) {
        return isBanned(player.getName());
    }

    // TODO: 19/12/2014 - R4zorax: UUID
    public boolean isBanned(String player) {
        return config.getStringList("banned.list").contains(player);
    }

    public void banPlayer(String player) {
        List<String> stringList = config.getStringList("banned.list");
        if (!stringList.contains(player)) {
            stringList.add(player);
        }
        config.set("banned.list", stringList);
        save();
    }

    public void unbanPlayer(String player) {
        List<String> stringList = config.getStringList("banned.list");
        while (stringList.contains(player)) {
            stringList.remove(player);
        }
        config.set("banned.list", stringList);
        save();
    }

    public void removeMember(String playername) {
        config.set("party.members." + playername, null);
        config.set("party.currentSize", getPartySize() - 1);
        save();
        sendMessageToIslandGroup(playername + " has been removed from the island group.");
    }

    public void setLevel(double score) {
        config.set("general.level", score);
        save();
    }

    public double getLevel() {
        return config.getDouble("general.level");
    }

    public void setRegionVersion(int version) {
        config.set("general.regionVersion", version);
        save();
    }

    public int getRegionVersion() {
        return config.getInt("general.regionVersion", 0);
    }

    public List<String> getLog() {
        List<String> log = new ArrayList<>();
        int cLog = config.getInt("log.logPos", 1);
        for (int i = 0; i < 10; i++) {
            String msg = config.getString("log." + (((cLog+i) % 10)+1), "");
            if (msg != null && !msg.trim().isEmpty()) {
                log.add(msg);
            }
        }
        return log;
    }

    public boolean isParty() {
        return getMembers().size() > 1;
    }

    public void setMaxPartySize(int size) {
        config.set("party.maxSize", size);
        save();
    }
}
