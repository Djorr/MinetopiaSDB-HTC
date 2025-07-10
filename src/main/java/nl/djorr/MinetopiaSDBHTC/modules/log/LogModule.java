package nl.djorr.MinetopiaSDBHTC.modules.log;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import nl.djorr.MinetopiaSDBHTC.modules.log.object.PlayerLog;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.LogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import nl.djorr.MinetopiaSDBHTC.modules.Module;
import nl.minetopiasdb.api.banking.Bankaccount;
import nl.minetopiasdb.api.banking.BankUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import nl.djorr.MinetopiaSDBHTC.modules.config.ConfigModule;
import lombok.Getter;

@Getter
public class LogModule implements Module {
    private final Map<UUID, PlayerLog> playerLogs = new HashMap<>();
    private final Gson gson;
    private File logsFolder;
    private Plugin plugin;

    public LogModule() {
        // Custom Gson met type adapters voor abstracte klassen
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        
        // Register custom serializer/deserializer for BalanceLogEntry
        builder.registerTypeAdapter(BalanceLogEntry.class, new BalanceLogEntry.Serializer());
        builder.registerTypeAdapter(BalanceLogEntry.class, new BalanceLogEntry.Deserializer());
        
        // Custom type adapter voor Bankaccount
        builder.registerTypeAdapter(Bankaccount.class, new JsonSerializer<Bankaccount>() {
            @Override
            public JsonElement serialize(Bankaccount src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == null) return JsonNull.INSTANCE;
                JsonObject obj = new JsonObject();
                obj.addProperty("id", src.getId());
                obj.addProperty("type", src.getType().name());
                obj.addProperty("balance", src.getBalance());
                if (src.getName() != null) {
                    obj.addProperty("name", src.getName());
                }
                return obj;
            }
        });
        
        builder.registerTypeAdapter(Bankaccount.class, new JsonDeserializer<Bankaccount>() {
            @Override
            public Bankaccount deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonNull()) return null;
                
                // Voor personal rekeningen (Essentials) returnen we null
                // omdat deze niet via SDB Bank API worden opgeslagen
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("type") && obj.get("type").getAsString().equals("PERSONAL")) {
                    return null; // Personal rekeningen gebruiken Essentials
                }
                
                // Voor andere rekening types proberen we de bankaccount op te halen
                if (obj.has("id")) {
                    int id = obj.get("id").getAsInt();
                    if (BankUtils.getInstance() != null) {
                        return BankUtils.getInstance().getBankAccount(id);
                    }
                }
                
                return null;
            }
        });
        
        // Custom type adapter voor Location
        builder.registerTypeAdapter(Location.class, new JsonSerializer<Location>() {
            @Override
            public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == null) return JsonNull.INSTANCE;
                JsonObject obj = new JsonObject();
                obj.addProperty("world", src.getWorld().getName());
                obj.addProperty("x", src.getX());
                obj.addProperty("y", src.getY());
                obj.addProperty("z", src.getZ());
                obj.addProperty("yaw", src.getYaw());
                obj.addProperty("pitch", src.getPitch());
                return obj;
            }
        });
        
        builder.registerTypeAdapter(Location.class, new JsonDeserializer<Location>() {
            @Override
            public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonNull()) return null;
                JsonObject obj = json.getAsJsonObject();
                String worldName = obj.get("world").getAsString();
                double x = obj.get("x").getAsDouble();
                double y = obj.get("y").getAsDouble();
                double z = obj.get("z").getAsDouble();
                float yaw = obj.get("yaw").getAsFloat();
                float pitch = obj.get("pitch").getAsFloat();
                return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            }
        });
        
        // Custom type adapter voor EnumMap in PlayerLog
        builder.registerTypeAdapter(new TypeToken<EnumMap<PlayerLogType, List<LogEntry>>>(){}.getType(), new JsonSerializer<EnumMap<PlayerLogType, List<LogEntry>>>() {
            @Override
            public JsonElement serialize(EnumMap<PlayerLogType, List<LogEntry>> src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject obj = new JsonObject();
                for (Map.Entry<PlayerLogType, List<LogEntry>> entry : src.entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        JsonArray array = new JsonArray();
                        for (LogEntry logEntry : entry.getValue()) {
                            array.add(context.serialize(logEntry));
                        }
                        obj.add(entry.getKey().name(), array);
                    }
                }
                return obj;
            }
        });
        
        builder.registerTypeAdapter(new TypeToken<EnumMap<PlayerLogType, List<LogEntry>>>(){}.getType(), new JsonDeserializer<EnumMap<PlayerLogType, List<LogEntry>>>() {
            @Override
            public EnumMap<PlayerLogType, List<LogEntry>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                EnumMap<PlayerLogType, List<LogEntry>> result = new EnumMap<>(PlayerLogType.class);
                
                // Initialize alle types
                for (PlayerLogType type : PlayerLogType.values()) {
                    result.put(type, new ArrayList<>());
                }
                
                if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        try {
                            PlayerLogType logType = PlayerLogType.valueOf(entry.getKey().toUpperCase());
                            JsonElement value = entry.getValue();
                            if (value.isJsonArray()) {
                                List<LogEntry> entries = new ArrayList<>();
                                for (JsonElement element : value.getAsJsonArray()) {
                                    if (element.isJsonObject()) {
                                        LogEntry logEntry = context.deserialize(element, BalanceLogEntry.class);
                                        if (logEntry != null) {
                                            entries.add(logEntry);
                                        }
                                    }
                                }
                                result.put(logType, entries);
                            }
                        } catch (IllegalArgumentException e) {
                            // Skip onbekende log types
                        }
                    }
                }
                
                return result;
            }
        });
        
        this.gson = builder.create();
    }

    @Override
    public void init(Plugin plugin) {
        this.plugin = plugin;
        logsFolder = new File(plugin.getDataFolder(), "LOGS");
        if (!logsFolder.exists()) logsFolder.mkdirs();
        loadAll();
        long saveInterval = ConfigModule.getInstance().getSaveIntervalMillis() / 50L; // ms to ticks
        long archiveInterval = ConfigModule.getInstance().getArchiveIntervalMillis() / 50L;
        if (saveInterval < 20) saveInterval = 20; // minimaal 1 seconde
        if (archiveInterval < 20) archiveInterval = 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, saveInterval, saveInterval);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::archiveOldLogs, archiveInterval, archiveInterval);
    }

    @Override
    public void shutdown() {
        saveAll();
    }

    public PlayerLog getPlayerLog(UUID speler) {
        if (speler == null) return null;
        PlayerLog log = playerLogs.get(speler);
        if (log == null) {
            log = new PlayerLog(speler);
            playerLogs.put(speler, log);
        }
        return log;
    }

    public void savePlayerLog(UUID speler) {
        PlayerLog log = getPlayerLog(speler);
        if (log == null) {
            return;
        }
        
        File file = getPlayerLogFile(speler);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            String json = gson.toJson(log);
            writer.write(json);
            // Removed per-player save log line
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        for (UUID uuid : playerLogs.keySet()) {
            try {
                savePlayerLog(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadAll() {
        if (logsFolder == null || !logsFolder.exists()) {
            return;
        }
        
        File[] uuidFolders = logsFolder.listFiles(File::isDirectory);
        if (uuidFolders == null) {
            return;
        }
        
        for (File uuidFolder : uuidFolders) {
            File file = new File(uuidFolder, "balancelog.json");
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    PlayerLog log = gson.fromJson(reader, PlayerLog.class);
                    if (log != null && log.getSpeler() != null) {
                        // Ensure logs map is properly initialized
                        if (log.getLogs() == null) {
                            log = new PlayerLog(log.getSpeler());
                        }
                        // Count total logs for debugging
                        int totalLogs = 0;
                        for (PlayerLogType type : PlayerLogType.values()) {
                            totalLogs += log.getLogs(type).size();
                        }
                        playerLogs.put(log.getSpeler(), log);
                        // Removed per-player load log line
                    } else {
                        // System.out.println("[MinetopiaSDB-HTC] Warning: Invalid log data in " + file.getPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JsonParseException e) {
                    // System.out.println("[MinetopiaSDB-HTC] Corrupt log file detected: " + file.getPath());
                    // Log bestand is corrupt of incompatibel, backup maken en overschrijven
                    try {
                        File backup = new File(file.getParentFile(), "balancelog.json.backup." + System.currentTimeMillis());
                        file.renameTo(backup);
                    } catch (Exception backupError) {
                        backupError.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // System.out.println("[MinetopiaSDB-HTC] No log file found for player folder: " + uuidFolder.getName());
            }
        }
        
    }

    public void archiveOldLogs() {
        for (UUID uuid : playerLogs.keySet()) {
            PlayerLog log = playerLogs.get(uuid);
            if (log == null) continue;
            List<?> toArchive = new ArrayList<>();
            long retentionMillis = ConfigModule.getInstance().getRetentionMillis();
            LocalDateTime cutoff = LocalDateTime.now().minusNanos(retentionMillis * 1000000L);
            for (nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry entry : log.getBalanceLogs()) {
                if (entry != null && entry.getTimestamp() != null && entry.getTimestamp().isBefore(cutoff)) {
                    ((List)toArchive).add(entry);
                }
            }
            if (!((List)toArchive).isEmpty()) {
                try {
                    File uuidFolder = new File(logsFolder, uuid.toString());
                    if (!uuidFolder.exists()) uuidFolder.mkdirs();
                    String archiveName = "BalanceLOG-archive-" + System.currentTimeMillis() + ".tar.gz";
                    File archiveFile = new File(uuidFolder, archiveName);
                    try (FileOutputStream fos = new FileOutputStream(archiveFile);
                         GZIPOutputStream gzos = new GZIPOutputStream(fos);
                         TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
                        String json = gson.toJson(toArchive);
                        byte[] data = json.getBytes(StandardCharsets.UTF_8);
                        TarArchiveEntry entry = new TarArchiveEntry("BalanceLOG-archive.json");
                        entry.setSize(data.length);
                        taos.putArchiveEntry(entry);
                        taos.write(data);
                        taos.closeArchiveEntry();
                    }
                    // Verwijder gearchiveerde logs uit actieve log
                    log.getBalanceLogs().removeAll((List)toArchive);
                    savePlayerLog(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File getPlayerLogFile(UUID uuid) {
        File uuidFolder = new File(logsFolder, uuid.toString().toLowerCase());
        if (!uuidFolder.exists()) uuidFolder.mkdirs();
        return new File(uuidFolder, "balancelog.json");
    }

    public UUID getUUID(String spelerNaam) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(spelerNaam);
        return offline != null ? offline.getUniqueId() : null;
    }

    public List<BalanceLogEntry> getBalanceLogs(String spelerNaam, int maxUrenTerug) {
        UUID uuid = getUUID(spelerNaam);
        if (uuid == null) return new ArrayList<>();
        PlayerLog log = getPlayerLog(uuid);
        if (log == null) return new ArrayList<>();
        long retentionMillis = ConfigModule.getInstance().getRetentionMillis();
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(retentionMillis * 1000000L);
        List<BalanceLogEntry> recent = new ArrayList<>();
        for (BalanceLogEntry entry : log.getBalanceLogs()) {
            if (entry != null && entry.getTimestamp() != null && entry.getTimestamp().isAfter(cutoff)) {
                recent.add(entry);
            }
        }
        return recent;
    }

    public List<BalanceLogEntry> getEssEconomyLogs(String spelerNaam, int maxUrenTerug) {
        UUID uuid = getUUID(spelerNaam);
        if (uuid == null) return new ArrayList<>();
        PlayerLog log = getPlayerLog(uuid);
        if (log == null) return new ArrayList<>();
        long retentionMillis = ConfigModule.getInstance().getRetentionMillis();
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(retentionMillis * 1000000L);
        List<BalanceLogEntry> recent = new ArrayList<>();
        for (BalanceLogEntry entry : log.getEssEconomyLogs()) {
            if (entry != null && entry.getTimestamp() != null && entry.getTimestamp().isAfter(cutoff)) {
                recent.add(entry);
            }
        }
        return recent;
    }

    public List<BalanceLogEntry> getLogsOfType(String spelerNaam, PlayerLogType type, int maxUrenTerug) {
        UUID uuid = getUUID(spelerNaam);
        if (uuid == null) {
            return new ArrayList<>();
        }
        PlayerLog log = getPlayerLog(uuid);
        if (log == null) {
            return new ArrayList<>();
        }
        long retentionMillis = ConfigModule.getInstance().getRetentionMillis();
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(retentionMillis * 1000000L);
        List<BalanceLogEntry> result = new ArrayList<>();
        List<LogEntry> logs = log.getLogs(type);
        for (LogEntry entry : logs) {
            if (entry != null && entry instanceof BalanceLogEntry) {
                if (entry.getTimestamp() != null && entry.getTimestamp().isAfter(cutoff)) {
                    result.add((BalanceLogEntry) entry);
                }
            }
        }
        return result;
    }
} 