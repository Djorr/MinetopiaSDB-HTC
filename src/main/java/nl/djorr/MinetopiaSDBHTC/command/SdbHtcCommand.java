package nl.djorr.MinetopiaSDBHTC.command;

import nl.djorr.MinetopiaSDBHTC.MinetopiaSDBHTC;
import nl.djorr.MinetopiaSDBHTC.modules.balance.BalanceModule;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import nl.djorr.MinetopiaSDBHTC.util.LogMenuUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SdbHtcCommand implements CommandExecutor {
    private static final int LOGS_PER_PAGE = 8;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final int HELP_PER_PAGE = 5;
    private static final String[][] HELP_PAGES = {
        {
            "§e/sdbhtc balhistory <speler>§7 - Bekijk de historie van een speler.",
        }
    };

    private void handleHelpCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minetopiasdbhtc.command.help")) {
            sender.sendMessage("§cJe hebt geen permissie om de help te bekijken.");
            return;
        }
        int pagina = 1;
        if (args.length >= 2) {
            try {
                pagina = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cPagina moet een getal zijn.");
                return;
            }
        }
        int totaalPaginas = (int) Math.ceil((double) HELP_PAGES[0].length / HELP_PER_PAGE);
        if (pagina < 1) pagina = 1;
        if (pagina > totaalPaginas) pagina = totaalPaginas;
        int start = (pagina - 1) * HELP_PER_PAGE;
        int end = Math.min(start + HELP_PER_PAGE, HELP_PAGES[0].length);
        sender.sendMessage("");
        sender.sendMessage("§6§lMinetopiaSDB-HTC Help (pagina " + pagina + "/" + totaalPaginas + ")");
        for (int i = start; i < end; i++) {
            sender.sendMessage(HELP_PAGES[0][i]);
        }
        sender.sendMessage("");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
            handleHelpCommand(sender, args);
            return true;
        }
        if (args.length < 2 || !(args[0].equalsIgnoreCase("balhistory") || args[0].equalsIgnoreCase("balhis"))) {
            if (sender.hasPermission("minetopiasdbhtc.command.balhistory") || sender.hasPermission("minetopiasdbhtc.command.balhis")) {
                sender.sendMessage("§cGebruik: /sdbhtc balhistory <speler> [pagina]");
            } else {
                sender.sendMessage("§cJe hebt geen permissie om dit commando te gebruiken.");
            }
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("balhistory") || sub.equals("balhis")) {
            if (!(sender.hasPermission("minetopiasdbhtc.command.balhistory") || sender.hasPermission("minetopiasdbhtc.command.balhis"))) {
                sender.sendMessage("§cJe hebt geen permissie om dit commando te gebruiken.");
                return true;
            }
            handleBallHistory(sender, args);
            return true;
        }
        return false;
    }

    private void handleBallHistory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cAlleen spelers kunnen dit menu openen.");
            return;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§cGebruik: /sdbhtc balhistory <speler>");
            return;
        }
        String spelerNaam = args[1];
        PlayerLogType type = PlayerLogType.ALL; // Default to ALL
        int pagina = 1;
        
        BalanceModule balanceModule = MinetopiaSDBHTC.getInstance().getBalanceModule();
        List<BalanceLogEntry> logs = balanceModule.getLogsOfType(spelerNaam, type, 24);
        LogMenuUtil.openLogMenu(player, spelerNaam, logs, type, pagina);
    }
} 