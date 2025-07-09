package nl.djorr.MinetopiaSDBHTC.modules.balance.listener;

import nl.minetopiasdb.api.banking.BankUtils;
import nl.minetopiasdb.api.enums.BankAccountType;
import nl.minetopiasdb.api.events.bank.BankAccountDepositEvent;
import nl.minetopiasdb.api.events.bank.BankAccountWithdrawEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import nl.djorr.MinetopiaSDBHTC.util.BalanceUtils;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import nl.djorr.MinetopiaSDBHTC.modules.balance.BalanceModule;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;

import java.util.UUID;

public class BalanceListener implements Listener {
    private final BalanceModule balanceModule;

    public BalanceListener(BalanceModule balanceModule) {
        this.balanceModule = balanceModule;
    }

    @EventHandler
    public void onDeposit(BankAccountDepositEvent event) {
        if (event.getPlayer() == null) {
            System.out.println("[MinetopiaSDB-HTC] Warning: Player is null in BankAccountDepositEvent");
            return;
        }
        
        UUID spelerUuid = event.getPlayer().getUniqueId();
        double gestort = event.getAmount();
        
        // Add null checks for BankUtils and bank account
        if (BankUtils.getInstance() == null) {
            System.out.println("[MinetopiaSDB-HTC] Warning: BankUtils.getInstance() is null in BankAccountDepositEvent");
            return;
        }
        
        nl.minetopiasdb.api.banking.Bankaccount bankAccount = BankUtils.getInstance().getBankAccount(event.getAccountId());
        if (bankAccount == null) {
            System.out.println("[MinetopiaSDB-HTC] Warning: Bank account is null for account ID: " + event.getAccountId());
            return;
        }
        
        double nieuwSaldo = bankAccount.getBalance();
        double oudSaldo = nieuwSaldo - gestort;
        BankAccountType rekeningType = event.getType();
        String actie = String.format("[SDB-BANK] %s ➜ %s: +€%.2f (Oud: €%.2f → Nieuw: €%.2f)", event.getPlayer().getName(), rekeningType, gestort, oudSaldo, nieuwSaldo);
        Location loc = event.getPlayer().getLocation();
        balanceModule.addBalanceLog(spelerUuid, actie, oudSaldo, nieuwSaldo, bankAccount, gestort, 0, null, loc);
    }

    @EventHandler
    public void onWithdraw(BankAccountWithdrawEvent event) {
        if (event.getPlayer() == null) {
            System.out.println("[MinetopiaSDB-HTC] Warning: Player is null in BankAccountWithdrawEvent");
            return;
        }
        
        UUID spelerUuid = event.getPlayer().getUniqueId();
        double opgenomen = event.getAmount();
        
        // Add null checks for BankUtils and bank account
        if (BankUtils.getInstance() == null) {
            System.out.println("[MinetopiaSDB-HTC] Warning: BankUtils.getInstance() is null in BankAccountWithdrawEvent");
            return;
        }
        
        nl.minetopiasdb.api.banking.Bankaccount bankAccount = BankUtils.getInstance().getBankAccount(event.getAccountId());
        if (bankAccount == null) {
            System.out.println("[MinetopiaSDB-HTC] Warning: Bank account is null for account ID: " + event.getAccountId());
            return;
        }
        
        double nieuwSaldo = bankAccount.getBalance();
        double oudSaldo = nieuwSaldo + opgenomen;
        BankAccountType rekeningType = event.getType();
        String actie = String.format("[SDB-BANK] %s ⇦ %s: -€%.2f (Oud: €%.2f → Nieuw: €%.2f)", event.getPlayer().getName(), rekeningType, opgenomen, oudSaldo, nieuwSaldo);
        Location loc = event.getPlayer().getLocation();
        balanceModule.addBalanceLog(spelerUuid, actie, oudSaldo, nieuwSaldo, bankAccount, -opgenomen, 0, null, loc);
    }

    @EventHandler
    public void onEcoCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        Player sender = event.getPlayer();
        if (!message.toLowerCase().startsWith("/eco ")) return;
        String[] args = message.split(" ");
        if (args.length < 3) return;
        String sub = args[1].toLowerCase();
        String targetName = args[2];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return;
        if (!sender.hasPermission("essentials.eco." + sub)) return;
        double oudSaldo = BalanceUtils.getPlayerCurrentSaldo(targetName);
        double nieuwSaldo;
        double bedrag = 0;
        String actie = "";
        String senderName = sender.getName();
        String targetNameCase = target.getName();
        if (sub.equals("give") || sub.equals("gift")) {
            if (args.length < 4) return;
            bedrag = BalanceUtils.parseDouble(args[3]);
            nieuwSaldo = oudSaldo + bedrag;
            actie = String.format("/eco give %s %.2f", targetNameCase, bedrag);
        } else if (sub.equals("take")) {
            if (args.length < 4) return;
            bedrag = BalanceUtils.parseDouble(args[3]);
            nieuwSaldo = oudSaldo - bedrag;
            actie = String.format("/eco take %s %.2f", targetNameCase, bedrag);
        } else if (sub.equals("set")) {
            if (args.length < 4) return;
            bedrag = BalanceUtils.parseDouble(args[3]);
            nieuwSaldo = bedrag;
            actie = String.format("/eco set %s %.2f", targetNameCase, bedrag);
        } else if (sub.equals("reset")) {
            nieuwSaldo = 0;
            actie = String.format("/eco reset %s", targetNameCase);
        } else {
            return;
        }
        if (!event.isCancelled()) {
            balanceModule.addBalanceLog(sender, target, actie, oudSaldo, nieuwSaldo, PlayerLogType.ESS_ECONOMY, null);
        }
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (BalanceUtils.isWitGeld(item)) {
            int waardePerStuk = BalanceUtils.getWitGeldWaarde(item);
            int aantal = item.getAmount();
            double totaal = waardePerStuk * aantal;
            Player player = event.getPlayer();
            UUID spelerUuid = player.getUniqueId();
            Location loc = event.getItem().getLocation();
            
            // Bereken huidige inventory saldo (zonder het item dat wordt opgepakt)
            double huidigeInventorySaldo = BalanceUtils.getAantalTotaalWitGeldWaarde(player);
            double oudSaldo = huidigeInventorySaldo; // Saldo voor pickup
            double nieuwSaldo = huidigeInventorySaldo + totaal; // Saldo na pickup
            
            String actie = String.format("[PICKUP] %s pakte %d x %s op: +€%.2f (locatie: %d,%d,%d %s)", player.getName(), aantal, item.getType().name(), totaal, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
            balanceModule.addPickupDropLog(spelerUuid, actie, oudSaldo, nieuwSaldo, PlayerLogType.PICKUP, totaal, aantal, item.getType().name(), loc);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (BalanceUtils.isWitGeld(item)) {
            int waardePerStuk = BalanceUtils.getWitGeldWaarde(item);
            int aantal = item.getAmount();
            double totaal = waardePerStuk * aantal;
            Player player = event.getPlayer();
            UUID spelerUuid = player.getUniqueId();
            Location loc = player.getLocation();
            
            // Bereken huidige inventory saldo (inclusief het item dat wordt gedropt)
            double huidigeInventorySaldo = BalanceUtils.getAantalTotaalWitGeldWaarde(player);
            double oudSaldo = huidigeInventorySaldo; // Saldo voor drop (inclusief item)
            double nieuwSaldo = huidigeInventorySaldo - totaal; // Saldo na drop (exclusief item)
            
            String actie = String.format("[DROP] %s dropte %d x %s: -€%.2f (locatie: %d,%d,%d %s)", player.getName(), aantal, item.getType().name(), totaal, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
            balanceModule.addPickupDropLog(spelerUuid, actie, oudSaldo, nieuwSaldo, PlayerLogType.DROP, totaal, aantal, item.getType().name(), loc);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        //TODO
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        //TODO
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        //TODO
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        //TODO
    }

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        //TODO
    }
} 