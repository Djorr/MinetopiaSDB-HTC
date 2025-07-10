package nl.djorr.MinetopiaSDBHTC;

import lombok.Getter;
import lombok.Setter;
import nl.djorr.MinetopiaSDBHTC.command.SdbHtcCommand;
import nl.djorr.MinetopiaSDBHTC.modules.log.LogModule;
import nl.djorr.MinetopiaSDBHTC.modules.balance.BalanceModule;
import nl.djorr.MinetopiaSDBHTC.util.LogMenuListener;
import nl.djorr.MinetopiaSDBHTC.modules.config.ConfigModule;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hoofdklasse van de MinetopiaSDB-HTC plugin.
 * Regelt lifecycle, module-initialisatie, command- en event-registratie.
 */
@Getter
public class MinetopiaSDBHTC extends JavaPlugin {
    @Getter
    private static MinetopiaSDBHTC instance;
    @Setter
    private LogModule logModule;
    @Setter
    private BalanceModule balanceModule;

    @Override
    public void onEnable() {
        instance = this;
        logBanner(true);
        initConfigModule();
        initModules();
        registerCommands();
        registerListeners();
        startMetrics();
        logStartupStats();
    }

    @Override
    public void onDisable() {
        logShutdownStats();
        shutdownModules();
        logBanner(false);
    }

    /**
     * Initialiseert de config module.
     */
    private void initConfigModule() {
        new ConfigModule(this);
    }

    /**
     * Initialiseert alle plugin modules.
     */
    private void initModules() {
        logModule = new LogModule();
        balanceModule = new BalanceModule(logModule);
        logModule.init(this);
        balanceModule.init(this);
    }

    /**
     * Registreert alle plugin commands.
     */
    private void registerCommands() {
        getCommand("sdbhtc").setExecutor(new SdbHtcCommand());
    }

    /**
     * Registreert alle event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new LogMenuListener(), this);
    }

    /**
     * Start bStats metrics.
     */
    private void startMetrics() {
        new Metrics(this, 26439);
    }

    /**
     * Logt statistieken na het opstarten.
     */
    private void logStartupStats() {
        int uniquePlayers = logModule.getPlayerLogs().size();
        int totalLogs = logModule.getPlayerLogs().values().stream().mapToInt(pl -> pl.getLogs().values().stream().mapToInt(java.util.List::size).sum()).sum();
        getLogger().info("§a[MinetopiaSDB-HTC] §fIngeladen logs: §e" + totalLogs + " §fvan §b" + uniquePlayers + " §funieke spelers.");
    }

    /**
     * Logt statistieken bij het afsluiten.
     */
    private void logShutdownStats() {
        int uniquePlayers = logModule != null ? logModule.getPlayerLogs().size() : 0;
        int totalLogs = logModule != null ? logModule.getPlayerLogs().values().stream().mapToInt(pl -> pl.getLogs().values().stream().mapToInt(java.util.List::size).sum()).sum() : 0;
        getLogger().info("§c[MinetopiaSDB-HTC] §fOpgeslagen logs: §e" + totalLogs + " §fvan §b" + uniquePlayers + " §funieke spelers.");
    }

    /**
     * Roept shutdown aan op alle modules.
     */
    private void shutdownModules() {
        if (balanceModule != null) balanceModule.shutdown();
        if (logModule != null) logModule.shutdown();
    }

    /**
     * Print een ASCII-banner bij opstarten en afsluiten.
     * @param startup true voor opstarten, false voor afsluiten
     */
    private void logBanner(boolean startup) {
        int uniquePlayers = logModule != null ? logModule.getPlayerLogs().size() : 0;
        int totalLogs = logModule != null ? logModule.getPlayerLogs().values().stream().mapToInt(pl -> pl.getLogs().values().stream().mapToInt(java.util.List::size).sum()).sum() : 0;
        String statsLine = startup
            ? "§f Ingeladen logs : §e" + totalLogs + " §fvan §b" + uniquePlayers + " §funieke spelers."
            : "§f Opgeslagen logs: §e" + totalLogs + " §fvan §b" + uniquePlayers + " §funieke spelers.";
        String[] banner = {
                "§6============================================",
                "§f Plugin : §bMinetopiaSDB-HTC",
                "§f Versie : §a" + getDescription().getVersion(),
                "§f Auteur : §d" + getDescription().getAuthors(),
                statsLine,
                "§6============================================",
                (startup ? "§a Plugin succesvol ingeschakeld!" : "§c Plugin uitgeschakeld. Alles veilig opgeslagen!"),
                "§6============================================"
        };
        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(line);
        }
    }
} 