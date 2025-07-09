package nl.djorr.MinetopiaSDBHTC.module;

import org.bukkit.plugin.Plugin;

public interface Module {
    void init(Plugin plugin);
    void shutdown();
} 