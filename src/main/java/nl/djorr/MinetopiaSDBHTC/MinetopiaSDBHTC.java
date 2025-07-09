package nl.djorr.MinetopiaSDBHTC;

import nl.djorr.MinetopiaSDBHTC.command.SdbHtcCommand;
import nl.djorr.MinetopiaSDBHTC.log.LogModule;
import nl.djorr.MinetopiaSDBHTC.balance.BalanceModule;
import nl.djorr.MinetopiaSDBHTC.util.LogMenuListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class MinetopiaSDBHTC extends JavaPlugin {
    private static MinetopiaSDBHTC instance;
    private LogModule logModule;
    private BalanceModule balanceModule;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("MinetopiaSDB-HTC plugin ingeschakeld!");
        // Init modules in juiste volgorde
        logModule = new LogModule();
        balanceModule = new BalanceModule(logModule);
        logModule.init(this);
        balanceModule.init(this);

        new Metrics(this, 26439);
        // Commands
        getCommand("sdbhtc").setExecutor(new SdbHtcCommand());
        // LogMenuListener
        getServer().getPluginManager().registerEvents(new LogMenuListener(), this);
    }

    @Override
    public void onDisable() {
        if (balanceModule != null) balanceModule.shutdown();
        if (logModule != null) logModule.shutdown();
        getLogger().info("MinetopiaSDB-HTC plugin uitgeschakeld!");
    }

    public static MinetopiaSDBHTC getInstance() {
        return instance;
    }

    public LogModule getLogModule() {
        return logModule;
    }

    public BalanceModule getBalanceModule() {
        return balanceModule;
    }

    public void setLogModule(LogModule logModule) {
        this.logModule = logModule;
    }

    public void setBalanceModule(BalanceModule balanceModule) {
        this.balanceModule = balanceModule;
    }
} 