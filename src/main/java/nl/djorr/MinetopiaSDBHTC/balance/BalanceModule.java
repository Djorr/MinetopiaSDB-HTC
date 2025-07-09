package nl.djorr.MinetopiaSDBHTC.balance;

import nl.djorr.MinetopiaSDBHTC.balance.listener.BalanceListener;
import nl.djorr.MinetopiaSDBHTC.log.LogModule;
import nl.djorr.MinetopiaSDBHTC.log.object.PlayerLog;
import nl.djorr.MinetopiaSDBHTC.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.log.type.PlayerLogType;
import nl.djorr.MinetopiaSDBHTC.module.Module;
import nl.djorr.MinetopiaSDBHTC.util.BalanceUtils;
import nl.minetopiasdb.api.banking.Bankaccount;
import nl.minetopiasdb.api.enums.BankAccountType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.djorr.MinetopiaSDBHTC.log.type.LogEntry;
import org.bukkit.Location;

public class BalanceModule implements Module {
    private final LogModule logModule;
    private Plugin plugin;

    public BalanceModule(LogModule logModule) {
        this.logModule = logModule;
    }

    @Override
    public void init(Plugin plugin) {
        this.plugin = plugin;
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new BalanceListener(this), plugin);
    }

    @Override
    public void shutdown() {
        // eventueel extra cleanup
        nl.djorr.MinetopiaSDBHTC.util.LogMenuUtil.shutdownLogMenus();
    }

    public void addBalanceLog(UUID speler, String actie, double oudSaldo, double nieuwSaldo, Bankaccount bankAccount, double amount, int itemAmount, String itemType, Location location) {
        PlayerLog log = logModule.getPlayerLog(speler);
        PlayerLogType logType = PlayerLogType.BALANCE;

        BalanceLogEntry entry = new BalanceLogEntry(
                speler, logType, actie, bankAccount, oudSaldo, nieuwSaldo, amount, itemAmount, itemType, location
        );

        log.getLogs(logType).add(entry);
        logModule.savePlayerLog(speler);
    }

    public void addBalanceLog(Player player, String actie, double oudSaldo, double nieuwSaldo, PlayerLogType type, Location location) {
        PlayerLog log = logModule.getPlayerLog(player.getUniqueId());

        switch (type) {
            case PICKUP:
            case DROP: {
                BalanceLogEntry entry = new BalanceLogEntry(
                        player.getUniqueId(), type, actie, oudSaldo, nieuwSaldo
                );
                if (location != null) entry.setLocation(location);
                log.getLogs(type).add(entry);
                logModule.savePlayerLog(player.getUniqueId());
                break;
            }
            case INVENTORY:
                break;
            case ESS_ECONOMY:
                break;
        }
    }

    public void addPickupDropLog(UUID uuid, String actie, double oudSaldo, double nieuwSaldo, PlayerLogType logType, double amount, int itemCount, String itemType, Location location) {
        if (uuid == null) return;
        PlayerLog log = logModule.getPlayerLog(uuid);
        BalanceLogEntry entry = new BalanceLogEntry(
                uuid, logType, actie
        );
        entry.setOudSaldo(oudSaldo);
        entry.setNieuwSaldo(nieuwSaldo);
        entry.setAmount(amount);
        entry.setItemAmount(itemCount);
        entry.setItemType(itemType);
        entry.setLocation(location);

        log.getLogs(logType).add(entry);
        logModule.savePlayerLog(uuid);
    }

    public List<BalanceLogEntry> getBalanceLogs(String spelerNaam, int maxUrenTerug) {
        UUID uuid = logModule.getUUID(spelerNaam);
        if (uuid == null) return new ArrayList<>();
        PlayerLog log = logModule.getPlayerLog(uuid);
        if (log == null) return new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxUrenTerug);
        List<BalanceLogEntry> recent = new ArrayList<>();
        for (BalanceLogEntry entry : log.getBalanceLogs()) {
            if (entry != null && entry.getTimestamp() != null && entry.getTimestamp().isAfter(cutoff)) {
                recent.add(entry);
            }
        }
        return recent;
    }

    // Generic log getter (voor toekomstige logtypes)
    public List<?> getLogs(String spelerNaam, String logType, int maxUrenTerug) {
        UUID uuid = logModule.getUUID(spelerNaam);
        if (uuid == null) return new ArrayList<>();
        PlayerLog log = logModule.getPlayerLog(uuid);
        if (log == null) return new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxUrenTerug);
        if (logType.equalsIgnoreCase("balance")) {
            List<BalanceLogEntry> recent = new ArrayList<>();
            for (BalanceLogEntry entry : log.getBalanceLogs()) {
                if (entry != null && entry.getTimestamp() != null && entry.getTimestamp().isAfter(cutoff)) {
                    recent.add(entry);
                }
            }
            return recent;
        }
        return new ArrayList<>();
    }

    public List<BalanceLogEntry> getLogsOfType(String spelerNaam, PlayerLogType type, int maxUrenTerug) {
        UUID uuid = logModule.getUUID(spelerNaam);
        if (uuid == null) {
            System.out.println("[MinetopiaSDB-HTC] UUID not found for player: " + spelerNaam);
            return new ArrayList<>();
        }
        PlayerLog log = logModule.getPlayerLog(uuid);
        if (log == null) {
            System.out.println("[MinetopiaSDB-HTC] PlayerLog not found for UUID: " + uuid);
            return new ArrayList<>();
        }
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxUrenTerug);
        List<BalanceLogEntry> result = new ArrayList<>();
        List<LogEntry> logs = log.getLogs(type);
        System.out.println("[MinetopiaSDB-HTC] Found " + logs.size() + " raw logs of type " + type + " for " + spelerNaam);
        for (LogEntry entry : logs) {
            if (entry != null && entry instanceof BalanceLogEntry) {
                // Timestamp filtering met langere periode (7 dagen in plaats van 24 uur)
                LocalDateTime cutoff7Days = LocalDateTime.now().minusDays(7);
                if (entry.getTimestamp() == null || entry.getTimestamp().isAfter(cutoff7Days)) {
                    result.add((BalanceLogEntry) entry);
                    System.out.println("[DEBUG] Added log entry: " + entry.getActie() + " timestamp: " + entry.getTimestamp());
                    // Extra debug voor pickup/drop logs
                    if (type == PlayerLogType.PICKUP || type == PlayerLogType.DROP) {
                        BalanceLogEntry balanceEntry = (BalanceLogEntry) entry;
                        System.out.println("[DEBUG] Pickup/Drop details - Amount: " + balanceEntry.getAmount() + ", ItemCount: " + balanceEntry.getItemAmount() + ", ItemType: " + balanceEntry.getItemType());
                    }
                }
            }
        }
        System.out.println("[MinetopiaSDB-HTC] Returning " + result.size() + " filtered logs of type " + type + " for " + spelerNaam);
        return result;
    }
}