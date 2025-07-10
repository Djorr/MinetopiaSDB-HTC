package nl.djorr.MinetopiaSDBHTC.modules.balance;

import nl.djorr.MinetopiaSDBHTC.modules.balance.listener.BalanceListener;
import nl.djorr.MinetopiaSDBHTC.modules.log.LogModule;
import nl.djorr.MinetopiaSDBHTC.modules.log.object.PlayerLog;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import nl.djorr.MinetopiaSDBHTC.modules.Module;
import nl.minetopiasdb.api.banking.Bankaccount;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.LogEntry;
import org.bukkit.Location;

import javax.annotation.Nullable;
import nl.djorr.MinetopiaSDBHTC.modules.config.ConfigModule;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BalanceModule implements Module {
    private final LogModule logModule;
    private Plugin plugin;

    private static final BlockingQueue<String> discordWebhookQueue = new LinkedBlockingQueue<>();
    private static volatile boolean discordWorkerStarted = false;

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

    private void startDiscordWebhookWorker() {
        if (discordWorkerStarted) return;
        discordWorkerStarted = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            while (true) {
                try {
                    String json = discordWebhookQueue.poll(1500, TimeUnit.MILLISECONDS); // 1.5s tussen berichten
                    if (json != null) {
                        sendDiscordWebhookRaw(json);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void sendDiscordWebhookRaw(String json) {
        String webhookUrl = ConfigModule.getInstance().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);
            conn.getInputStream().close();
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("[MinetopiaSDB-HTC] Fout bij versturen log naar Discord webhook: " + e.getMessage());
        }
    }

    private void sendLogToDiscord(BalanceLogEntry entry) {
        String webhookUrl = ConfigModule.getInstance().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        // Tagging logica
        boolean tagRole = false;
        String roleId = ConfigModule.getInstance().getWhitelistgeldRole();
        // Tag staff als bedrag (abs) >= 50000 euro
        if (Math.abs(entry.getAmount()) >= 50000 && roleId != null && !roleId.isEmpty()) tagRole = true;
        StringBuilder content = new StringBuilder();
        if (tagRole) content.append(roleId).append(" ");
        // Spelernaam ophalen
        String spelerNaam = "Onbekend";
        if (entry.getSpeler() != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getSpeler());
            if (offlinePlayer != null && offlinePlayer.getName() != null) {
                spelerNaam = offlinePlayer.getName();
            }
        }
        // Originele eigenaar ophalen
        String originalOwnerName = null;
        if (entry.getTarget() != null) {
            OfflinePlayer orig = Bukkit.getOfflinePlayer(entry.getTarget());
            if (orig != null && orig.getName() != null) originalOwnerName = orig.getName();
        }
        // Target spelernaam (voor eco commando's)
        String targetNaam = null;
        if (entry.getTarget() != null && entry.getLogType() == PlayerLogType.ESS_ECONOMY) {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(entry.getTarget());
            if (targetPlayer != null && targetPlayer.getName() != null) {
                targetNaam = targetPlayer.getName();
            }
        }
        String title = entry.getLogType() != null ? entry.getLogType().name() : "Log";
        StringBuilder desc = new StringBuilder();
        DateTimeFormatter tijdFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter datumFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int color = 0x4FC3F7; // default: licht blauw
        switch (entry.getLogType()) {
            case PICKUP:
            case DROP:
                color = 0x81C784; // licht groen
                break;
            case ESS_ECONOMY:
                color = 0xFFF176; // licht geel
                break;
            case BALANCE:
            default:
                color = 0x4FC3F7; // licht blauw
                break;
        }
        switch (entry.getLogType()) {
            case DROP:
                desc.append("**Speler:** ").append(spelerNaam).append("\n");
                if (entry.getItemAmount() != 0 && entry.getItemType() != null) {
                    desc.append("**Items:** ").append(entry.getItemAmount()).append("x ").append(entry.getItemType()).append("\n");
                }
                // Toon bedrag met minteken voor euro bij droppen
                if (entry.getAmount() < 0) {
                    desc.append("**Totaal bedrag:** -€").append(Math.abs(entry.getAmount())).append("\n");
                } else {
                    desc.append("**Totaal bedrag:** €").append(entry.getAmount()).append("\n");
                }
                if (entry.getLocation() != null) {
                    desc.append("**Locatie:** x").append(entry.getLocation().getBlockX())
                        .append(" y").append(entry.getLocation().getBlockY())
                        .append(" z").append(entry.getLocation().getBlockZ())
                        .append(" ").append(entry.getLocation().getWorld().getName()).append("\n");
                    desc.append("**Command:** /tppos ")
                        .append(entry.getLocation().getBlockX()).append(" ")
                        .append(entry.getLocation().getBlockY()).append(" ")
                        .append(entry.getLocation().getBlockZ()).append(" ")
                        .append(entry.getLocation().getWorld().getName()).append("\n");
                }
                desc.append("**Tijd:** ")
                    .append(entry.getTimestamp().format(datumFormatter)).append(" ")
                    .append(entry.getTimestamp().format(tijdFormatter)).append("\n");
                break;
            case PICKUP:
                desc.append("**Speler:** ").append(spelerNaam).append("\n");
                if (originalOwnerName != null) desc.append("**Gedropt door:** ").append(originalOwnerName).append("\n");
                if (entry.getItemAmount() != 0 && entry.getItemType() != null) {
                    desc.append("**Items:** ").append(entry.getItemAmount()).append("x ").append(entry.getItemType()).append("\n");
                }
                // Toon bedrag met plusteken voor euro bij pickup
                if (entry.getAmount() < 0) {
                    desc.append("**Totaal bedrag:** -€").append(Math.abs(entry.getAmount())).append("\n");
                } else {
                    desc.append("**Totaal bedrag:** €").append(entry.getAmount()).append("\n");
                }
                if (entry.getLocation() != null) {
                    desc.append("**Locatie:** x").append(entry.getLocation().getBlockX())
                        .append(" y").append(entry.getLocation().getBlockY())
                        .append(" z").append(entry.getLocation().getBlockZ())
                        .append(" ").append(entry.getLocation().getWorld().getName()).append("\n");
                    desc.append("**Command:** /tppos ")
                        .append(entry.getLocation().getBlockX()).append(" ")
                        .append(entry.getLocation().getBlockY()).append(" ")
                        .append(entry.getLocation().getBlockZ()).append(" ")
                        .append(entry.getLocation().getWorld().getName()).append("\n");
                }
                desc.append("**Tijd:** ")
                    .append(entry.getTimestamp().format(datumFormatter)).append(" ")
                    .append(entry.getTimestamp().format(tijdFormatter)).append("\n");
                break;
            case BALANCE:
                desc.append("**Speler:** ").append(spelerNaam).append("\n");
                String type = entry.getAmount() >= 0 ? "Storten" : "Opnemen";
                desc.append("**Type:** ").append(type).append("\n");
                if (entry.getBankaccount() != null) {
                    String rekeningType = nl.djorr.MinetopiaSDBHTC.util.BalanceUtils.getRekeningNaam(entry.getBankaccount().getType());
                    desc.append("**Rekening:** ").append(rekeningType).append(" ID: ").append(entry.getBankaccount().getId()).append("\n");
                }
                // Toon bedrag met minteken voor euro bij opnemen
                if (entry.getAmount() < 0) {
                    desc.append("**Bedrag:** -€").append(Math.abs(entry.getAmount())).append("\n");
                } else {
                    desc.append("**Bedrag:** €").append(entry.getAmount()).append("\n");
                }
                desc.append("**Oud saldo:** €").append(entry.getOudSaldo()).append("\n");
                desc.append("**Nieuw saldo:** €").append(entry.getNieuwSaldo());
                double verschil = entry.getNieuwSaldo() - entry.getOudSaldo();
                if (verschil < 0) {
                    desc.append(" (-€").append(Math.abs(verschil)).append(")\n");
                } else {
                    desc.append(" (+€").append(Math.abs(verschil)).append(")\n");
                }
                desc.append("**Tijdstip:** ")
                    .append(entry.getTimestamp().format(datumFormatter)).append(" ")
                    .append(entry.getTimestamp().format(tijdFormatter)).append("\n");
                break;
            case ESS_ECONOMY:
                desc.append("**Speler:** ").append(spelerNaam).append("\n");
                if (targetNaam != null) desc.append("**Target:** ").append(targetNaam).append("\n");
                // Toon bedrag met minteken voor euro bij take
                if (entry.getAmount() < 0) {
                    desc.append("**Totaal bedrag:** -€").append(Math.abs(entry.getAmount())).append("\n");
                } else {
                    desc.append("**Totaal bedrag:** €").append(entry.getAmount()).append("\n");
                }
                desc.append("**Tijd:** ")
                    .append(entry.getTimestamp().format(datumFormatter)).append(" ")
                    .append(entry.getTimestamp().format(tijdFormatter)).append("\n");
                break;
            default:
                desc.append(entry.getActie()).append("\n");
                desc.append("**Tijd:** ")
                    .append(entry.getTimestamp().format(datumFormatter)).append(" ")
                    .append(entry.getTimestamp().format(tijdFormatter)).append("\n");
                break;
        }
        // Embed JSON
        String embedJson = "{\"embeds\":[{" +
                "\"title\": " + escapeJson(title) + "," +
                "\"description\": " + escapeJson(desc.toString()) + "," +
                "\"color\": " + color +
                "}]}";
        String json;
        if (content.length() > 0) {
            json = "{\"content\": " + escapeJson(content.toString()) + "," + embedJson.substring(1);
        } else {
            json = embedJson;
        }
        // Start de worker als die nog niet draait
        startDiscordWebhookWorker();
        // Voeg bericht toe aan de queue
        discordWebhookQueue.offer(json);
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    public void addBalanceLog(UUID speler, String actie, double oudSaldo, double nieuwSaldo, Bankaccount bankAccount, double amount, int itemAmount, String itemType, Location location) {
        try {
            if (speler == null) {
                return;
            }
            
        PlayerLog log = logModule.getPlayerLog(speler);
            if (log == null) {
                return;
            }
            
        PlayerLogType logType = PlayerLogType.BALANCE;

        BalanceLogEntry entry = new BalanceLogEntry(
                speler, logType, actie, bankAccount, oudSaldo, nieuwSaldo, amount, itemAmount, itemType, location
        );

        log.getLogs(logType).add(entry);
        logModule.savePlayerLog(speler);
            sendLogToDiscord(entry);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBalanceLog(Player player, @Nullable Player target, String actie, double oudSaldo, double nieuwSaldo, PlayerLogType type, Location location) {
        try {
            if (player == null) {
                return;
            }
            
        PlayerLog log = logModule.getPlayerLog(player.getUniqueId());
            if (log == null) {
                return;
            }

        switch (type) {
            case PICKUP:
            case DROP: {
                BalanceLogEntry entry = new BalanceLogEntry(
                        player.getUniqueId(), type, actie, oudSaldo, nieuwSaldo
                );
                if (target != null) entry.setTarget(target.getUniqueId());
                if (location != null) entry.setLocation(location);
                log.getLogs(type).add(entry);
                logModule.savePlayerLog(player.getUniqueId());
                    sendLogToDiscord(entry);
                break;
            }
            case INVENTORY:
                break;
            case ESS_ECONOMY: {
                BalanceLogEntry entry = new BalanceLogEntry(
                        player.getUniqueId(), type, actie, oudSaldo, nieuwSaldo
                );
                if (target != null) entry.setTarget(target.getUniqueId());
                if (location != null) entry.setLocation(location);
                log.getLogs(type).add(entry);
                logModule.savePlayerLog(player.getUniqueId());
                    sendLogToDiscord(entry);
                break;
            }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPickupDropLog(UUID uuid, String actie, double oudSaldo, double nieuwSaldo, PlayerLogType logType, double amount, int itemCount, String itemType, Location location, UUID originalOwner) {
        try {
            if (uuid == null) {
                return;
            }
        PlayerLog log = logModule.getPlayerLog(uuid);
            if (log == null) {
                return;
            }
        BalanceLogEntry entry = new BalanceLogEntry(
                uuid, logType, actie
        );
        entry.setOudSaldo(oudSaldo);
        entry.setNieuwSaldo(nieuwSaldo);
        entry.setAmount(amount);
        entry.setItemAmount(itemCount);
        entry.setItemType(itemType);
        entry.setLocation(location);
            entry.setTarget(originalOwner); // Gebruik target als originalOwner voor pickup
        log.getLogs(logType).add(entry);
        logModule.savePlayerLog(uuid);
            sendLogToDiscord(entry);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    
    public List<BalanceLogEntry> getEssEconomyLogs(String spelerNaam, int maxUrenTerug) {
        UUID uuid = logModule.getUUID(spelerNaam);
        if (uuid == null) return new ArrayList<>();
        PlayerLog log = logModule.getPlayerLog(uuid);
        if (log == null) return new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxUrenTerug);
        List<BalanceLogEntry> recent = new ArrayList<>();
        for (BalanceLogEntry entry : log.getEssEconomyLogs()) {
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
            return new ArrayList<>();
        }
        PlayerLog log = logModule.getPlayerLog(uuid);
        if (log == null) {
            return new ArrayList<>();
        }
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxUrenTerug);
        List<BalanceLogEntry> result = new ArrayList<>();
        List<LogEntry> logs = log.getLogs(type);
        for (LogEntry entry : logs) {
            if (entry != null && entry instanceof BalanceLogEntry) {
                // Timestamp filtering met langere periode (7 dagen in plaats van 24 uur)
                LocalDateTime cutoff7Days = LocalDateTime.now().minusDays(7);
                if (entry.getTimestamp() == null || entry.getTimestamp().isAfter(cutoff7Days)) {
                    result.add((BalanceLogEntry) entry);
                }
            }
        }
        return result;
    }
}