package nl.djorr.MinetopiaSDBHTC.util;

import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LogMenuListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        LogMenuUtil.LogMenuState state = LogMenuUtil.getMenuState(player);
        if (state == null) return;
        Inventory inv = event.getInventory();
        // Herken menu aan nieuwe titel
        if (inv.getTitle() == null || !inv.getTitle().contains("(HTC) ")) return;
        event.setCancelled(true); // altijd read-only, geen drag, geen pickup, geen verplaatsing
        // Alleen linkermuiskliks verwerken
        if (event.getClick() == null || !event.getClick().isLeftClick()) return;
        int slot = event.getRawSlot();
        // Categorieknoppen mapping
        PlayerLogType clickedType;
        if (slot == 0) clickedType = PlayerLogType.ALL;
        else if (slot == 2) clickedType = PlayerLogType.BALANCE;
        else if (slot == 3) clickedType = PlayerLogType.PICKUP;
        else if (slot == 4) clickedType = PlayerLogType.INVENTORY;
        else if (slot == 5) clickedType = PlayerLogType.ESS_ECONOMY;
        else {
            clickedType = null;
        }
        if (clickedType != null) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MinetopiaSDB-HTC"), () -> {
                player.closeInventory();
                LogMenuUtil.openLogMenu(player, state.spelerNaam, LogMenuUtil.getAllLogs(state.spelerNaam), clickedType, 1);
            }, 1L);
            return;
        }
        // Navigatie (slot 45 = vorige, 53 = volgende)
        if (slot == 45 && state.pagina > 1) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MinetopiaSDB-HTC"), () -> {
                player.closeInventory();
                LogMenuUtil.openLogMenu(player, state.spelerNaam, LogMenuUtil.getAllLogs(state.spelerNaam), state.type, state.pagina - 1);
            }, 1L);
            return;
        }
        if (slot == 53) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MinetopiaSDB-HTC"), () -> {
                player.closeInventory();
                LogMenuUtil.openLogMenu(player, state.spelerNaam, LogMenuUtil.getAllLogs(state.spelerNaam), state.type, state.pagina + 1);
            }, 1L);
            return;
        }
        // Boekje met locatie: teleport
        if (slot >= 9 && slot < 45) {
            ItemStack clicked = inv.getItem(slot);
            if (clicked != null && clicked.getType() == org.bukkit.Material.BOOK && clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                java.util.List<String> lore = clicked.getItemMeta().getLore();
                if (lore != null) {
                    for (String line : lore) {
                        if (line.contains("Left-click to teleport")) {
                            if (!player.hasPermission("minetopiasdbhtc.teleportlog")) {
                                player.sendMessage("§cJe hebt geen permissie om naar deze locatie te teleporteren.");
                                return;
                            }
                            // Zoek locatie in lore
                            String locLine = null;
                            for (String l : lore) if (l.contains("Locatie:")) locLine = l;
                            if (locLine != null) {
                                try {
                                    String[] parts = locLine.replace("§bLocatie: §f[", "").replace("]", "").split(" ");
                                    int x = 0, y = 64, z = 0;
                                    String world = player.getWorld().getName();
                                    for (String part : parts) {
                                        if (part.startsWith("x")) x = Integer.parseInt(part.substring(1));
                                        else if (part.startsWith("y")) y = Integer.parseInt(part.substring(1));
                                        else if (part.startsWith("z")) z = Integer.parseInt(part.substring(1));
                                        else world = part;
                                    }
                                    org.bukkit.Location loc = new org.bukkit.Location(Bukkit.getWorld(world), x, y, z);
                                    player.closeInventory();
                                    player.teleport(loc);
                                    player.sendMessage("§aJe bent geteleporteerd naar de locatie van deze log.");
                                } catch (Exception e) {
                                    player.sendMessage("§cKon locatie niet parsen uit deze log.");
                                }
                            }
                            return;
                        }
                    }
                }
            }
        }
        // Alle andere slots: altijd gecanceld, geen interactie
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        LogMenuUtil.clearMenuState(player);
    }
} 