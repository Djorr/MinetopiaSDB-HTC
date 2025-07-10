package nl.djorr.MinetopiaSDBHTC.util;

import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import nl.minetopiasdb.api.banking.BankUtils;
import nl.minetopiasdb.api.banking.Bankaccount;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

public class LogMenuUtil {
    private static final int ROWS = 6;
    private static final int LOGS_PER_PAGE = 36; // 4x9 (row 2-5)
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Map<UUID, LogMenuState> openMenus = new HashMap<>();

    public static void openLogMenu(Player viewer, String spelerNaam, List<BalanceLogEntry> allLogs, PlayerLogType selectedType, int pagina) {
        // Filter logs op type
        List<BalanceLogEntry> logs = new ArrayList<>();
        if (selectedType == null) selectedType = PlayerLogType.ALL;
        if (selectedType == PlayerLogType.ALL) {
            logs.addAll(allLogs);
        } else if (selectedType == PlayerLogType.PICKUP) {
            for (BalanceLogEntry entry : allLogs) {
                if (entry.getLogType() == PlayerLogType.PICKUP || entry.getLogType() == PlayerLogType.DROP) logs.add(entry);
            }
        } else if (selectedType == PlayerLogType.BALANCE) {
            for (BalanceLogEntry entry : allLogs) {
                if (entry.getLogType() == PlayerLogType.BALANCE) logs.add(entry);
            }
        } else if (selectedType == PlayerLogType.INVENTORY) {
            for (BalanceLogEntry entry : allLogs) {
                if (entry.getLogType() == PlayerLogType.INVENTORY) logs.add(entry);
            }
        } else if (selectedType == PlayerLogType.ESS_ECONOMY) {
            for (BalanceLogEntry entry : allLogs) {
                if (entry.getLogType() == PlayerLogType.ESS_ECONOMY) logs.add(entry);
            }
        }
        // Sorteer logs van nieuw naar oud (met null-safe timestamp handling)
        logs.sort((a, b) -> {
            LocalDateTime timestampA = a.getTimestamp();
            LocalDateTime timestampB = b.getTimestamp();
            
            // Als beide null zijn, gelijk
            if (timestampA == null && timestampB == null) return 0;
            // Als A null is, A komt later (nieuwe logs zonder timestamp)
            if (timestampA == null) return 1;
            // Als B null is, B komt later (nieuwe logs zonder timestamp)
            if (timestampB == null) return -1;
            // Anders normale vergelijking
            return timestampB.compareTo(timestampA);
        });
        int totaalPaginas = Math.max(1, (int) Math.ceil(logs.size() / (double) LOGS_PER_PAGE));
        if (pagina < 1) pagina = 1;
        if (pagina > totaalPaginas) pagina = totaalPaginas;
        int start = (pagina - 1) * LOGS_PER_PAGE;
        int end = Math.min(start + LOGS_PER_PAGE, logs.size());
        // Titel: (HTC) naam (categorie)
        String title = ChatColor.GOLD + "(HTC) " + ChatColor.YELLOW + spelerNaam + ChatColor.GRAY + " (" + getCategoryDisplayName(selectedType) + ")";
        Inventory inv = Bukkit.createInventory(null, ROWS * 9, title);
        // 1. Border (grijze glass pane op eerste en laatste rij)
        ItemStack border = new ItemBuilder(Material.STAINED_GLASS_PANE).setDurability((short) 7).setName(" ").toItemStack();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        // 2. Categorieknoppen op vaste slots
        inv.setItem(0, getCategoryItem(PlayerLogType.ALL, selectedType));
        inv.setItem(2, getCategoryItem(PlayerLogType.BALANCE, selectedType));
        inv.setItem(3, getCategoryItem(PlayerLogType.PICKUP, selectedType));
        inv.setItem(4, getCategoryItem(PlayerLogType.INVENTORY, selectedType));
        inv.setItem(5, getCategoryItem(PlayerLogType.ESS_ECONOMY, selectedType));
        // 3. Logboekjes (rij 2-5, slots 9-44)
        int logSlot = 9;
        for (int i = start; i < end && logSlot < 45; i++, logSlot++) {
            BalanceLogEntry entry = logs.get(i);
            inv.setItem(logSlot, createLogBookItem(entry));
        }
        // 4. Navigatie (slot 45 = vorige, 49 = info, 53 = volgende, override border)
        if (pagina > 1) {
            inv.setItem(45, new ItemBuilder(Material.ARROW).setName("§eVorige pagina").toItemStack());
        }
        if (pagina < totaalPaginas) {
            inv.setItem(53, new ItemBuilder(Material.ARROW).setName("§eVolgende pagina").toItemStack());
        }
        // Info in het midden van de laatste rij
        inv.setItem(49, new ItemBuilder(Material.PAPER).setName("§7Pagina §e" + pagina + "§7/§e" + totaalPaginas).toItemStack());
        // Open menu en onthoud state
        viewer.openInventory(inv);
        openMenus.put(viewer.getUniqueId(), new LogMenuState(spelerNaam, selectedType, pagina));
    }

    public static LogMenuState getMenuState(Player player) {
        return openMenus.get(player.getUniqueId());
    }
    public static void clearMenuState(Player player) {
        openMenus.remove(player.getUniqueId());
    }

    public static List<BalanceLogEntry> getAllLogs(String spelerNaam) {
        // Haal alle logs van alle types op (voor menu)
        List<BalanceLogEntry> all = new ArrayList<>();
        for (PlayerLogType type : PlayerLogType.values()) {
            List<BalanceLogEntry> logs = nl.djorr.MinetopiaSDBHTC.MinetopiaSDBHTC.getInstance().getBalanceModule().getLogsOfType(spelerNaam, type, 24);
            all.addAll(logs);
        }
        return all;
    }

    public static ItemStack createLogBookItem(BalanceLogEntry entry) {
        if (entry == null) {
            return new ItemBuilder(org.bukkit.Material.BOOK).setName("§cNull Entry").setLore("§7This log entry is null").toItemStack();
        }
        String tijd = entry.getTimestamp() != null ? entry.getTimestamp().format(FORMATTER) : "";
        String kleur = getTypeColor(entry.getLogType());
        String title = kleur + "[" + (entry.getLogType() != null ? entry.getLogType().name() : "UNKNOWN") + "] §7" + tijd;
        List<String> lore = formatLogLoreLines(entry, entry.getLogType());
        return new ItemBuilder(org.bukkit.Material.BOOK).setName(title).setLore(lore).toItemStack();
    }

    private static List<String> formatLogLoreLines(BalanceLogEntry entry, PlayerLogType type) {
        List<String> lore = new ArrayList<>();
        if (entry == null) {
            lore.add("§cNull entry");
            return lore;
        }
        String spelerName = "Onbekend";
        if (entry.getSpeler() != null) {
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(entry.getSpeler());
            spelerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Onbekend";
        }
        String speler = "§e" + spelerName + "§7";

        OfflinePlayer targetPlayer = null;
        if (entry.getTarget() != null) {
            targetPlayer = Bukkit.getOfflinePlayer(entry.getTarget());
        }
        switch (type) {
            case BALANCE:
                String rekeningNaam = "";
                boolean hasStructured = entry.getOudSaldo() != 0.0 || entry.getNieuwSaldo() != 0.0 || entry.getAmount() != 0.0;
                if (entry.getBankaccount() == null && hasStructured) {
                    // Personal rekening (Essentials balance)
                        rekeningNaam = "§aPrivérekening";
                        double bedrag = Math.abs(entry.getNieuwSaldo() - entry.getOudSaldo());
                        String changedAmount;
                        if (entry.getNieuwSaldo() > entry.getOudSaldo()) {
                        changedAmount = "§e(+€" + formatAmount(bedrag) + ")";
                            lore.add(speler + " heeft §a+§a€" + formatAmount(bedrag) + "§7 gestort.");
                        } else {
                        changedAmount = "§e(-€" + formatAmount(bedrag) + ")";
                            lore.add(speler + " heeft §c-§c€" + formatAmount(bedrag) + " §7opgenomen.");
                        }
                        lore.add("");
                        lore.add("§fRekening gegevens:");
                    lore.add("§7Naam: §2" + rekeningNaam + " §8(Essentials Balance)");
                        lore.add("§7Oud saldo: §c€" + formatAmount(entry.getOudSaldo()));
                        lore.add("§7Nieuw saldo: §a€" + formatAmount(entry.getNieuwSaldo()) + " " + changedAmount);
                } else if (entry.getBankaccount() != null && hasStructured) {
                    switch (entry.getBankaccount().getType()) {
                        case PERSONAL: rekeningNaam = "§aPrivérekening"; break;
                        case SAVINGS: rekeningNaam = "§eSpaarrekening"; break;
                        case BUSINESS: rekeningNaam = "§bBedrijfsrekening"; break;
                        case GOVERNMENT: rekeningNaam = "§4Overheidsrekening"; break;
                        default: rekeningNaam = "§7Onbekend"; break;
                    }
                        double bedrag = Math.abs(entry.getNieuwSaldo() - entry.getOudSaldo());
                        String changedAmount;
                        if (entry.getNieuwSaldo() > entry.getOudSaldo()) {
                        changedAmount = "§e(+€" + formatAmount(bedrag) + ")";
                            lore.add(speler + " heeft §a+§a€" + formatAmount(bedrag) + "§7 gestort.");
                        } else {
                        changedAmount = "§e(-€" + formatAmount(bedrag) + ")";
                            lore.add(speler + " heeft §c-§c€" + formatAmount(bedrag) + " §7opgenomen.");
                        }
                        lore.add("");
                        lore.add("§fRekening gegevens:");
                    lore.add("§7Naam: " + rekeningNaam + " §8(ID: " + entry.getBankaccount().getId() + ")");
                        lore.add("§7Oud saldo: §c€" + formatAmount(entry.getOudSaldo()));
                    lore.add("§7Nieuw saldo: §a€" + formatAmount(entry.getNieuwSaldo()) + " " + changedAmount);
                        } else {
                    // Fallback: geen gestructureerde data, toon actie-string
                    lore.add(speler + " §7heeft een bank-actie uitgevoerd:");
                    lore.add("§7" + entry.getActie());
                        lore.add("");
                        lore.add("§7Oud saldo: §c€" + formatAmount(entry.getOudSaldo()));
                    lore.add("§7Nieuw saldo: §a€" + formatAmount(entry.getNieuwSaldo()));
                }
                break;
            case PICKUP:
            case DROP:
                String actie = entry.getLogType() == PlayerLogType.PICKUP ? "opgepakt" : "gedropt";
                String kleur = entry.getLogType() == PlayerLogType.PICKUP ? "§a" : "§c";
                String prefix = entry.getLogType() == PlayerLogType.PICKUP ? "+" : "-";

                double perStuk = entry.getItemAmount() > 0 ? (entry.getAmount() / entry.getItemAmount()) : entry.getAmount();
                String perStukStr = formatAmount(perStuk);

                String totaalStr = formatAmount(entry.getAmount());

                lore.add(speler + " heeft §e" + entry.getItemAmount() + "x §e€" + perStukStr + " §fwit geld§7 " + actie + ".");
                lore.add("§7Totale waarde: " + kleur + prefix + "€" + totaalStr);
                lore.add("");

                if (entry.getLocation() != null) {
                    lore.add("§bLocatie: §fx" + entry.getLocation().getBlockX() + " y" + entry.getLocation().getBlockY() + " z" + entry.getLocation().getBlockZ() + " " + entry.getLocation().getWorld().getName());
                    lore.add("");
                    lore.add("§eLeft-click to teleport to this location");
                }
                break;
            case INVENTORY:
                boolean creative = entry.getActie().toLowerCase().contains("creative");
                lore.addAll(splitLoreLines(speler + " heeft " + (creative ? "§c[Creative] " : "") + "een inventory-actie uitgevoerd:"));
                lore.addAll(splitLoreLines("§7" + entry.getActie()));
                break;
            case ESS_ECONOMY:
                double bedrag = Math.abs(entry.getNieuwSaldo() - entry.getOudSaldo());
                String changedAmount;
                if (entry.getNieuwSaldo() > entry.getOudSaldo()) {
                    changedAmount = "§e+€" + formatAmount(bedrag);
                } else {
                    changedAmount = "§e-€" + formatAmount(bedrag);
                }

                lore.add(speler + " §7heeft een Essentials Economy-actie uitgevoerd:");
                lore.add(speler + " §7heeft " + entry.getActie());
                lore.add("");
                if (targetPlayer != null) {
                    lore.add("§f" + targetPlayer.getName() + " rekening gegevens:");
                }
                lore.add("§7Oud saldo: §c€" + formatAmount(entry.getOudSaldo()));
                lore.add("§7Nieuw saldo: §a€" + formatAmount(entry.getNieuwSaldo()) + " §e(" + changedAmount + ")");
                break;
        }
        return lore;
    }

    private static String getTypeColor(PlayerLogType type) {
        if (type == null) return "§7";
        switch (type) {
            case BALANCE: return "§b";
            case PICKUP: return "§a";
            case DROP: return "§c";
            case INVENTORY: return "§d";
            case ESS_ECONOMY: return "§e";
            default: return "§7";
        }
    }

    private static String extractLocatie(String actie) {
        int idx = actie.indexOf("(locatie:");
        if (idx != -1) {
            return actie.substring(idx).replace("(locatie:", "(x").replace(",", " y").replace(",", " z").replace(")", ")");
        }
        return "";
    }

    private static Material getCategoryMaterial(PlayerLogType type) {
        switch (type) {
            case BALANCE: return Material.BOOK;
            case PICKUP: return Material.EMERALD;
            case DROP: return Material.REDSTONE;
            case INVENTORY: return Material.CHEST;
            case ESS_ECONOMY: return Material.GOLD_INGOT;
            default: return Material.PAPER;
        }
    }

    private static ItemStack getCategoryItem(PlayerLogType type, PlayerLogType selected) {
        Material mat;
        String display;
        List<String> lore = new ArrayList<>();
        boolean glow = false;
        switch (type) {
            case ALL:
                mat = Material.BOOK_AND_QUILL;
                display = "§aAlle Logs";
                lore.add("");
                lore.add("§7Toont alle logtypes in één overzicht.");
                lore.add("");
                lore.add("§eKlik hier om te filteren.");
                break;
            case BALANCE:
                mat = Material.BOOK;
                display = "§bRekeningen";
                lore.add("");
                lore.add("§7Alle stortingen en opnames.");
                lore.add("");
                lore.add("§eKlik hier om te filteren.");
                break;
            case PICKUP:
                mat = Material.EMERALD;
                display = "§aPickup & Drop";
                lore.add("");
                lore.add("§7Alle oppak/drop acties.");
                lore.add("");
                lore.add("§eKlik hier om te filteren.");
                break;
            case INVENTORY:
                mat = Material.CHEST;
                display = "§dInventory Changes";
                lore.add("");
                lore.add("§7Alle inventory verplaatsingen.");
                lore.add("");
                lore.add("§eKlik hier om te filteren.");
                glow = (selected == PlayerLogType.INVENTORY); // GLOW als geselecteerd
                break;
            case ESS_ECONOMY:
                mat = Material.GOLD_INGOT;
                display = "§eEssentials Economy";
                lore.add("");
                lore.add("§7Alle Essentials economy acties.");
                lore.add("");
                lore.add("§eKlik hier om te filteren.");
                break;
            default:
                mat = Material.PAPER;
                display = "§7Onbekend";
                break;
        }
        ItemBuilder builder = new ItemBuilder(mat).setName(display).setLore(lore);
        if (glow) builder.addGlow();
        return builder.toItemStack();
    }

    private static String getCategoryDisplayName(PlayerLogType type) {
        switch (type) {
            case ALL: return "Alle Logs";
            case BALANCE: return "Rekeningen";
            case PICKUP: return "Pickup en Drops";
            case INVENTORY: return "Inventory Changes";
            case ESS_ECONOMY: return "Essentials Economy";
            default: return "Onbekend";
        }
    }

    private static String highlightRekeningType(String type) {
        if (type == null) return "§fOnbekend";
        switch (type.toLowerCase()) {
            case "privé":
            case "prive": return "§bPrivé";
            case "spaar": return "§dSpaar";
            case "bedrijf": return "§6Bedrijf";
            case "overheid": return "§cOverheid";
            default: return "§f" + type;
        }
    }

    private static String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }

    private static String extractRekeningId(String actie) {
        int idIdx = actie.indexOf("ID: ");
        if (idIdx != -1) {
            int end = actie.indexOf(",", idIdx);
            if (end == -1) end = actie.indexOf(")", idIdx);
            if (end == -1) end = actie.length();
            return actie.substring(idIdx + 4, end).trim();
        }
        return null;
    }

    public static void shutdownLogMenus() {
        for (UUID uuid : openMenus.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getOpenInventory() != null) {
                p.closeInventory();
            }
        }
        openMenus.clear();
    }

    // State class voor menu
    public static class LogMenuState {
        public final String spelerNaam;
        public final PlayerLogType type;
        public final int pagina;
        public LogMenuState(String spelerNaam, PlayerLogType type, int pagina) {
            this.spelerNaam = spelerNaam;
            this.type = type;
            this.pagina = pagina;
        }
    }

    // Utility: split lange lore regels automatisch op in max 40 tekens per regel
    private static List<String> splitLoreLines(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) return result;
        int max = 40;
        StringBuilder sb = new StringBuilder();
        for (String word : line.split(" ")) {
            if (sb.length() + word.length() + 1 > max) {
                result.add(ChatColor.translateAlternateColorCodes('&', sb.toString()));
                sb = new StringBuilder();
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(word);
        }
        if (sb.length() > 0) result.add(ChatColor.translateAlternateColorCodes('&', sb.toString()));
        return result;
    }
} 