package nl.djorr.MinetopiaSDBHTC.modules.log;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import nl.djorr.MinetopiaSDBHTC.modules.log.object.PlayerLog;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.LogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import nl.djorr.MinetopiaSDBHTC.modules.Module;
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

public class LogModule implements Module {
    private final Map<UUID, PlayerLog> playerLogs = new HashMap<>();
    private final Gson gson;
    private File logsFolder;
    private Plugin plugin;

    public LogModule() {
        // Custom Gson met type adapters voor abstracte klassen
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        
        // Custom type adapter voor LogEntry (abstracte klasse)
        builder.registerTypeAdapter(LogEntry.class, new JsonDeserializer<LogEntry>() {
            @Override
            public LogEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonObject = json.getAsJsonObject();
                String logTypeStr = jsonObject.get("logType") != null ? jsonObject.get("logType").getAsString() : "BALANCE";
                PlayerLogType logType = PlayerLogType.valueOf(logTypeStr);
                
                // Voor nu, altijd BalanceLogEntry gebruiken
                return context.deserialize(json, BalanceLogEntry.class);
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
                            System.out.println("[MinetopiaSDB-HTC] Unknown log type: " + entry.getKey());
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
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 20*60*5, 20*60*5);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::archiveOldLogs, 20*60*10, 20*60*10);
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
            System.out.println("[MinetopiaSDB-HTC] Created new PlayerLog for " + speler);
        }
        return log;
    }

    public void savePlayerLog(UUID speler) {
        PlayerLog log = getPlayerLog(speler);
        if (log == null) return;
        File file = getPlayerLogFile(speler);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(log, writer);
            System.out.println("[MinetopiaSDB-HTC] Saved log for " + speler + " to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        for (UUID uuid : playerLogs.keySet()) {
            savePlayerLog(uuid);
        }
    }

    public void loadAll() {
        if (logsFolder == null || !logsFolder.exists()) return;
        File[] uuidFolders = logsFolder.listFiles(File::isDirectory);
        if (uuidFolders == null) return;
        for (File uuidFolder : uuidFolders) {
            File file = new File(uuidFolder, "BalanceLOG.json");
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    PlayerLog log = gson.fromJson(reader, PlayerLog.class);
                    if (log != null && log.getSpeler() != null) {
                        // Ensure logs map is properly initialized
                        if (log.getLogs() == null) {
                            log = new PlayerLog(log.getSpeler());
                        }
                        playerLogs.put(log.getSpeler(), log);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JsonParseException e) {
                    // Log bestand is corrupt of incompatibel, backup maken en overschrijven
                    System.out.println("[MinetopiaSDB-HTC] Corrupt log file detected: " + file.getAbsolutePath());
                    try {
                        File backup = new File(file.getParentFile(), "BalanceLOG.json.backup." + System.currentTimeMillis());
                        file.renameTo(backup);
                        System.out.println("[MinetopiaSDB-HTC] Backup created: " + backup.getAbsolutePath());
                    } catch (Exception backupError) {
                        backupError.printStackTrace();
                    }
                } catch (Exception e) {
                    // Andere errors, gewoon loggen
                    System.out.println("[MinetopiaSDB-HTC] Error loading log file: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    public void archiveOldLogs() {
        for (UUID uuid : playerLogs.keySet()) {
            PlayerLog log = playerLogs.get(uuid);
            if (log == null) continue;
            List<?> toArchive = new ArrayList<>();
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            for (nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry entry : log.getBalanceLogs()) {
                if (entry.getTimestamp().isBefore(cutoff)) {
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
        File uuidFolder = new File(logsFolder, uuid.toString());
        if (!uuidFolder.exists()) uuidFolder.mkdirs();
        return new File(uuidFolder, "BalanceLOG.json");
    }

    public UUID getUUID(String spelerNaam) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(spelerNaam);
        return offline != null ? offline.getUniqueId() : null;
    }
} 