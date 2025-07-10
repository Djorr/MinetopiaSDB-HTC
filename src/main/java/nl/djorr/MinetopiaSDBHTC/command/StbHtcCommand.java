package nl.djorr.MinetopiaSDBHTC.command;

import nl.djorr.MinetopiaSDBHTC.MinetopiaSDBHTC;
import nl.djorr.MinetopiaSDBHTC.modules.balance.BalanceModule;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;
import nl.djorr.MinetopiaSDBHTC.util.LogMenuUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class StbHtcCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cAlleen spelers kunnen dit menu openen.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cGebruik: /stbhtc <speler>");
            return true;
        }
        Player player = (Player) sender;
        String spelerNaam = args[0];
        PlayerLogType type = PlayerLogType.ALL;
        int pagina = 1;
        BalanceModule balanceModule = MinetopiaSDBHTC.getInstance().getBalanceModule();
        List<BalanceLogEntry> logs = balanceModule.getLogsOfType(spelerNaam, type, 24);
        LogMenuUtil.openLogMenu(player, spelerNaam, logs, type, pagina);
        return true;
    }
} 