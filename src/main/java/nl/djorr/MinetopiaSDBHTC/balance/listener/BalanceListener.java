package nl.djorr.MinetopiaSDBHTC.balance.listener;

import nl.minetopiasdb.api.banking.BankUtils;
import nl.minetopiasdb.api.banking.Bankaccount;
import nl.minetopiasdb.api.enums.BankAccountType;
import nl.minetopiasdb.api.events.bank.BankAccountDepositEvent;
import nl.minetopiasdb.api.events.bank.BankAccountWithdrawEvent;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.HumanEntity;
import nl.djorr.MinetopiaSDBHTC.balance.BalanceModule;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import nl.djorr.MinetopiaSDBHTC.log.type.PlayerLogType;

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
        double nieuwSaldo = oudSaldo;
        double bedrag = 0;
        String actie = "";
        String senderName = sender.getName();
        String targetNameCase = target.getName();
        if (sub.equals("give") || sub.equals("gift")) {
            if (args.length < 4) return;
            bedrag = BalanceUtils.parseDouble(args[3]);
            nieuwSaldo = oudSaldo + bedrag;
            actie = String.format("/eco give " + targetNameCase + " " + bedrag, senderName, targetNameCase, bedrag, oudSaldo, nieuwSaldo);
        } else if (sub.equals("take")) {
            if (args.length < 4) return;
            bedrag = BalanceUtils.parseDouble(args[3]);
            nieuwSaldo = oudSaldo - bedrag;
            actie = String.format("/eco take " + targetNameCase + " " + bedrag, senderName, targetNameCase, bedrag, oudSaldo, nieuwSaldo);
        } else if (sub.equals("set")) {
            if (args.length < 4) return;
            bedrag = BalanceUtils.parseDouble(args[3]);
            nieuwSaldo = bedrag;
            actie = String.format("/eco set " + targetNameCase + " " + bedrag, senderName, targetNameCase, bedrag, oudSaldo, nieuwSaldo);
        } else if (sub.equals("reset")) {
            nieuwSaldo = 0;
            actie = String.format("/eco reset  " + targetNameCase, senderName, targetNameCase, oudSaldo, nieuwSaldo);
        } else {
            return;
        }
        if (!event.isCancelled()) {
            balanceModule.addBalanceLog(target, actie, oudSaldo, nieuwSaldo, PlayerLogType.ESS_ECONOMY, null);
        }
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (BalanceUtils.isWitGeld(item)) {
            int waardePerStuk = BalanceUtils.getWitGeldWaarde(item);
            int aantal = item.getAmount();
            double totaal = waardePerStuk * aantal;
            UUID spelerUuid = event.getPlayer().getUniqueId();
            Location loc = event.getItem().getLocation();
            String actie = String.format("[PICKUP] %s pakte %d x %s op: +€%.2f (locatie: %d,%d,%d %s)", event.getPlayer().getName(), aantal, item.getType().name(), totaal, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
            balanceModule.addPickupDropLog(spelerUuid, actie, 0, totaal, PlayerLogType.PICKUP, totaal, aantal, item.getType().name(), loc);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (BalanceUtils.isWitGeld(item)) {
            int waardePerStuk = BalanceUtils.getWitGeldWaarde(item);
            int aantal = item.getAmount();
            double totaal = waardePerStuk * aantal;
            UUID spelerUuid = event.getPlayer().getUniqueId();
            Location loc = event.getPlayer().getLocation();
            String actie = String.format("[DROP] %s dropte %d x %s: -€%.2f (locatie: %d,%d,%d %s)", event.getPlayer().getName(), aantal, item.getType().name(), totaal, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
            balanceModule.addPickupDropLog(spelerUuid, actie, totaal, 0, PlayerLogType.DROP, totaal, aantal, item.getType().name(), loc);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!BalanceUtils.isWitGeld(item)) return;
        final UUID lastOwner = BalanceUtils.getLastOwner(item);
        String lastOwnerName = lastOwner != null ? org.bukkit.Bukkit.getOfflinePlayer(lastOwner).getName() : "Onbekend";
        String actor = event.getInventory().getHolder() != null ? event.getInventory().getHolder().getClass().getSimpleName() : "Onbekend";
        Location loc = event.getItem().getLocation();
        if (actor.toLowerCase().contains("hopper")) {
            String actie = String.format("[DROP] %s dropte wit geld in een hopper (UUID: %s, naam: %s)", lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend", lastOwnerName);
            balanceModule.addPickupDropLog(lastOwner, actie, 0, BalanceUtils.getWitGeldWaarde(item), PlayerLogType.DROP, BalanceUtils.getWitGeldWaarde(item), item.getAmount(), item.getType().name(), loc);
        } else {
            String actie = String.format("[INVENTORY] %s pakte wit geld op uit %s (UUID: %s, naam: %s)", actor, actor, lastOwner != null ? lastOwner.toString() : "Onbekend", lastOwnerName);
            balanceModule.addPickupDropLog(lastOwner, actie, 0, BalanceUtils.getWitGeldWaarde(item), PlayerLogType.INVENTORY, BalanceUtils.getWitGeldWaarde(item), item.getAmount(), item.getType().name(), loc);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (!BalanceUtils.isWitGeld(item)) return;
        final UUID lastOwner = BalanceUtils.getLastOwner(item);
        String lastOwnerName = lastOwner != null ? org.bukkit.Bukkit.getOfflinePlayer(lastOwner).getName() : "Onbekend";
        String sourceType = "Onbekend";
        Location sourceLoc = null;
        if (event.getSource().getHolder() != null && BalanceUtils.isBlockInventory(event.getSource().getHolder())) {
            org.bukkit.block.BlockState block = (org.bukkit.block.BlockState) event.getSource().getHolder();
            sourceType = block.getType().name().toLowerCase();
            sourceLoc = block.getLocation();
        } else if (event.getSource().getHolder() instanceof org.bukkit.entity.HumanEntity) {
            sourceType = "player inventory";
        }
        String destType = "Onbekend";
        Location destLoc = null;
        if (event.getDestination().getHolder() != null && BalanceUtils.isBlockInventory(event.getDestination().getHolder())) {
            org.bukkit.block.BlockState block = (org.bukkit.block.BlockState) event.getDestination().getHolder();
            destType = block.getType().name().toLowerCase();
            destLoc = block.getLocation();
        } else if (event.getDestination().getHolder() instanceof org.bukkit.entity.HumanEntity) {
            destType = "player inventory";
        }
        String actie = String.format("[INVENTORY] wit geld verplaatst van %s %s naar %s %s (oorspronkelijk van: %s, UUID: %s)", sourceType, sourceLoc != null ? locToString(sourceLoc) : "", destType, destLoc != null ? locToString(destLoc) : "", lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend");
        balanceModule.addPickupDropLog(lastOwner, actie, 0, BalanceUtils.getWitGeldWaarde(item), PlayerLogType.INVENTORY, BalanceUtils.getWitGeldWaarde(item), item.getAmount(), item.getType().name(), destLoc);
    }

    private String locToString(Location loc) {
        return String.format("[x%d y%d z%d %s]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        ItemStack item = event.getCurrentItem();
        if (!BalanceUtils.isWitGeld(item)) return;
        UUID spelerUuid = null;
        if (event.getWhoClicked() instanceof Player) {
            spelerUuid = ((Player) event.getWhoClicked()).getUniqueId();
        }
        final UUID lastOwner = BalanceUtils.getLastOwner(item);
        String lastOwnerName = lastOwner != null ? org.bukkit.Bukkit.getOfflinePlayer(lastOwner).getName() : "Onbekend";
        // Zet altijd de lastowner-tag als een speler wit geld in een blok-inventory plaatst
        if (event.getClickedInventory() != null && BalanceUtils.isBlockInventory(event.getClickedInventory().getHolder())) {
            ItemStack tagged = BalanceUtils.setLastOwner(item, spelerUuid);
            event.setCurrentItem(tagged);
        }
        int waarde = BalanceUtils.getWitGeldWaarde(item);
        Inventory clickedInv = event.getClickedInventory();
        String invInfo = "";
        int locX = 0, locY = 0, locZ = 0;
        String world = null;
        String itemType = item.getType().name();
        int itemCount = item.getAmount();
        boolean isBlockInventory = false;
        String blockLoc = "";
        if (clickedInv != null && BalanceUtils.isBlockInventory(clickedInv.getHolder())) {
            InventoryHolder holder = clickedInv.getHolder();
            org.bukkit.block.BlockState block = (org.bukkit.block.BlockState) holder;
            org.bukkit.Location loc = block.getLocation();
            invInfo = block.getType().name().toLowerCase();
            locX = loc.getBlockX();
            locY = loc.getBlockY();
            locZ = loc.getBlockZ();
            world = loc.getWorld().getName();
            isBlockInventory = true;
            blockLoc = String.format("[x%d y%d z%d %s]", locX, locY, locZ, world);
        }
        String actie = "";
        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY:
                if (isBlockInventory) {
                    actie = String.format("[INVENTORY] %s plaatste wit geld van zijn inventory in %s %s (oorspronkelijk van: %s, UUID: %s)", event.getWhoClicked().getName(), invInfo, blockLoc, lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend");
                    balanceModule.addPickupDropLog(spelerUuid, actie, 0, waarde, PlayerLogType.INVENTORY, waarde, itemCount, itemType, new org.bukkit.Location(org.bukkit.Bukkit.getWorld(world), locX, locY, locZ));
                }
                break;
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PICKUP_SOME:
                if (isBlockInventory) {
                    actie = String.format("[INVENTORY] %s haalde wit geld uit %s %s naar zijn inventory (oorspronkelijk van: %s, UUID: %s)", event.getWhoClicked().getName(), invInfo, blockLoc, lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend");
                    balanceModule.addPickupDropLog(spelerUuid, actie, waarde, 0, PlayerLogType.INVENTORY, waarde, itemCount, itemType, new org.bukkit.Location(org.bukkit.Bukkit.getWorld(world), locX, locY, locZ));
                }
                break;
            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:
                if (isBlockInventory) {
                    actie = String.format("[INVENTORY] %s dropte wit geld uit %s %s op de grond (oorspronkelijk van: %s, UUID: %s)", event.getWhoClicked().getName(), invInfo, blockLoc, lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend");
                    balanceModule.addPickupDropLog(spelerUuid, actie, waarde, 0, PlayerLogType.INVENTORY, waarde, itemCount, itemType, new org.bukkit.Location(org.bukkit.Bukkit.getWorld(world), locX, locY, locZ));
                }
                break;
            default:
                return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (ItemStack item : event.getNewItems().values()) {
            if (!BalanceUtils.isWitGeld(item)) continue;
            UUID spelerUuid = null;
            if (event.getWhoClicked() instanceof Player) {
                spelerUuid = ((Player) event.getWhoClicked()).getUniqueId();
            }
            final UUID lastOwner = BalanceUtils.getLastOwner(item);
            String lastOwnerName = lastOwner != null ? org.bukkit.Bukkit.getOfflinePlayer(lastOwner).getName() : "Onbekend";
            // Detect drag naar blok-inventory
            if (event.getInventory() != null && BalanceUtils.isBlockInventory(event.getInventory().getHolder())) {
                org.bukkit.block.BlockState block = (org.bukkit.block.BlockState) event.getInventory().getHolder();
                org.bukkit.Location loc = block.getLocation();
                String invInfo = block.getType().name().toLowerCase();
                String blockLoc = String.format("[x%d y%d z%d %s]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
                int waarde = BalanceUtils.getWitGeldWaarde(item);
                int itemCount = item.getAmount();
                String actie = String.format("[INVENTORY] %s plaatste wit geld van zijn inventory in %s %s (oorspronkelijk van: %s, UUID: %s)", event.getWhoClicked().getName(), invInfo, blockLoc, lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend");
                balanceModule.addPickupDropLog(spelerUuid, actie, 0, waarde, PlayerLogType.INVENTORY, waarde, itemCount, item.getType().name(), loc);
            }
        }
    }

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        ItemStack item = event.getCursor();
        if (!BalanceUtils.isWitGeld(item)) return;
        UUID spelerUuid = null;
        if (event.getWhoClicked() instanceof Player) {
            spelerUuid = ((Player) event.getWhoClicked()).getUniqueId();
        }
        final UUID lastOwner = BalanceUtils.getLastOwner(item);
        String lastOwnerName = lastOwner != null ? org.bukkit.Bukkit.getOfflinePlayer(lastOwner).getName() : "Onbekend";
        // Detect creative interactie met blok-inventory
        if (event.getInventory() != null && BalanceUtils.isBlockInventory(event.getInventory().getHolder())) {
            org.bukkit.block.BlockState block = (org.bukkit.block.BlockState) event.getInventory().getHolder();
            org.bukkit.Location loc = block.getLocation();
            String invInfo = block.getType().name().toLowerCase();
            String blockLoc = String.format("[x%d y%d z%d %s]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
            int waarde = BalanceUtils.getWitGeldWaarde(item);
            int itemCount = item.getAmount();
            String actie = String.format("[INVENTORY] %s plaatste wit geld van zijn inventory in %s %s (oorspronkelijk van: %s, UUID: %s)", event.getWhoClicked().getName(), invInfo, blockLoc, lastOwnerName, lastOwner != null ? lastOwner.toString() : "Onbekend");
            balanceModule.addPickupDropLog(spelerUuid, actie, 0, waarde, PlayerLogType.INVENTORY, waarde, itemCount, item.getType().name(), loc);
        }
    }
} 