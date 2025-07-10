package nl.djorr.MinetopiaSDBHTC.modules.config;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ConfigModule {
    @Getter
    private static ConfigModule instance;
    private final JavaPlugin plugin;
    private FileConfiguration config;

    @Setter
    private long retentionMillis;
    @Setter
    private long saveIntervalMillis;
    @Setter
    private long archiveIntervalMillis;
    @Setter
    private String webhookUrl;
    @Setter
    private String whitelistgeldRole;
    @Setter
    private int whitelistgeldMinimum;

    public ConfigModule(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.retentionMillis = parseTime(config.getString("retention_period", "24h"));
        this.saveIntervalMillis = parseTime(config.getString("save_interval", "5m"));
        this.archiveIntervalMillis = parseTime(config.getString("archive_interval", "10m"));
        this.webhookUrl = config.getString("webhook_url", "");
        this.whitelistgeldRole = config.getString("whitelistgeld_role", "");
        this.whitelistgeldMinimum = config.getInt("whitelistgeld_minimum", 32);
    }

    /**
     * Parse a time string like '24h', '5m', '30s' to milliseconds.
     */
    public static long parseTime(String timeStr) {
        if (timeStr == null) return 0L;
        timeStr = timeStr.trim().toLowerCase();
        try {
            if (timeStr.endsWith("h")) {
                return Long.parseLong(timeStr.replace("h", "")) * 60 * 60 * 1000;
            } else if (timeStr.endsWith("m")) {
                return Long.parseLong(timeStr.replace("m", "")) * 60 * 1000;
            } else if (timeStr.endsWith("s")) {
                return Long.parseLong(timeStr.replace("s", "")) * 1000;
            } else {
                // fallback: try parse as millis
                return Long.parseLong(timeStr);
            }
        } catch (Exception e) {
            return 0L;
        }
    }
} 