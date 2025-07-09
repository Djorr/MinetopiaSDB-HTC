package nl.djorr.MinetopiaSDBHTC.modules;

import org.bukkit.plugin.Plugin;

public interface Module {
    void init(Plugin plugin);
    void shutdown();
} 