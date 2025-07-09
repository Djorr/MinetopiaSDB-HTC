package nl.djorr.MinetopiaSDBHTC.util;

import com.earth2me.essentials.User;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import net.ess3.api.IEssentials;
import nl.minetopiasdb.api.enums.BankAccountType;

import java.util.UUID;

public class BalanceUtils {
    public static boolean isWitGeld(ItemStack item) {
        return NBTEditor.contains(item, "sdb_realmoney");
    }

    public static int getWitGeldWaarde(ItemStack item) {
        if (item == null || !isWitGeld(item)) return 0;
        Material type = item.getType();
        switch (type) {
            case GHAST_TEAR: return 5000;
            case DIAMOND: return 2000;
            case REDSTONE: return 1000;
            case EMERALD: return 500;
            case COAL: return 200;
            case IRON_INGOT: return 100;
            case QUARTZ: return 50;
            case GOLD_INGOT: return 10;
            case GOLD_NUGGET: return 1;
            default: return 0;
        }
    }

    public static double getEssentialsBalance(String spelerNaam) {
        Player player = Bukkit.getPlayerExact(spelerNaam);
        if (player == null) return 0.0;
        IEssentials essentials = (IEssentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        if (essentials == null) return 0.0;
        User user = essentials.getUser(player);
        if (user == null) return 0.0;
        return user.getMoney().doubleValue();
    }

    public static double getPlayerCurrentSaldo(String spelerNaam) {
        Player player = Bukkit.getPlayerExact(spelerNaam);
        if (player == null) return 0.0;
        IEssentials essentials = (IEssentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        if (essentials == null) return 0.0;
        User user = essentials.getUser(player);
        if (user == null) return 0.0;
        return user.getMoney().doubleValue();
    }

    public static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static String getRekeningNaam(BankAccountType type) {
        if (type == null) return "Onbekend";
        switch (type) {
            case PERSONAL:
                return "Privé";
            case SAVINGS:
                return "Spaar";
            case BUSINESS:
                return "Bedrijf";
            case GOVERNMENT:
                return "Overheid";
            default:
                return "Onbekend";
        }
    }

    public static ItemStack setLastOwner(ItemStack item, UUID spelerUuid) {
        if (item == null || spelerUuid == null) return item;
        return NBTEditor.set(item, spelerUuid.toString(), "sdb_lastowner");
    }
    public static ItemStack setLastOwner(ItemStack item, String spelerNaam) {
        if (item == null) return null;
        Player player = Bukkit.getPlayerExact(spelerNaam);
        if (player != null) {
            return setLastOwner(item, player.getUniqueId());
        }
        return NBTEditor.set(item, spelerNaam, "sdb_lastowner");
    }
    public static UUID getLastOwner(ItemStack item) {
        if (item == null) return null;
        if (!NBTEditor.contains(item, "sdb_lastowner")) return null;
        Object tag = NBTEditor.getString(item, "sdb_lastowner");
        if (tag == null) return null;
        try {
            return UUID.fromString(tag.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isBlockInventory(org.bukkit.inventory.InventoryHolder holder) {
        if (holder == null) return false;
        // Check for all known block inventory types in 1.12.2
        return holder instanceof org.bukkit.block.Chest
            || holder instanceof org.bukkit.block.DoubleChest
            || holder instanceof org.bukkit.block.Hopper
            || holder instanceof org.bukkit.block.CommandBlock
            || holder instanceof org.bukkit.block.NoteBlock
            || holder instanceof org.bukkit.block.Sign
            || holder instanceof org.bukkit.block.Skull
            || holder instanceof org.bukkit.block.Dispenser
            || holder instanceof org.bukkit.block.Dropper
            || holder instanceof org.bukkit.block.Furnace
            || holder instanceof org.bukkit.block.BrewingStand
            || holder instanceof org.bukkit.block.ShulkerBox
            || holder instanceof org.bukkit.block.Jukebox
            || holder instanceof org.bukkit.block.Beacon
            || holder instanceof org.bukkit.block.EnderChest;
    }
} 