package nl.djorr.MinetopiaSDBHTC.modules.log.type;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import nl.minetopiasdb.api.banking.Bankaccount;
import org.bukkit.Location;

@Setter
@Getter
@Data
@EqualsAndHashCode(callSuper = true)
public class BalanceLogEntry extends LogEntry {
    private double oudSaldo;
    private double nieuwSaldo;
    private double amount; // altijd aanwezig


    // Temp data
    private Bankaccount bankaccount;
    private int itemAmount; // voor pickup/drop
    private String itemType; // voor pickup/drop
    private Location location;
    private UUID target;

    public BalanceLogEntry(UUID speler, PlayerLogType logType, String actie) {
        super(speler, logType, actie, LocalDateTime.now());
    }

    public BalanceLogEntry(UUID speler, PlayerLogType logType, String actie, Bankaccount bankaccount, double oudSaldo, double nieuwSaldo, double amount, int itemAmount, String itemType, Location location) {
        super(speler, logType, actie, LocalDateTime.now());

        this.bankaccount = bankaccount;
        this.oudSaldo = oudSaldo;
        this.nieuwSaldo = nieuwSaldo;
        this.amount = amount;

        this.itemAmount = itemAmount;
        this.itemType = itemType;
        this.location = location;
        this.target = null;
    }

    public BalanceLogEntry(UUID speler, PlayerLogType logType, String actie,double oudSaldo, double nieuwSaldo) {
        super(speler, logType, actie, LocalDateTime.now());

        this.oudSaldo = oudSaldo;
        this.nieuwSaldo = nieuwSaldo;
    }

    public BalanceLogEntry(UUID speler, PlayerLogType logType, String actie,double oudSaldo, double nieuwSaldo, double amount) {
        super(speler, logType, actie, LocalDateTime.now());

        this.oudSaldo = oudSaldo;
        this.nieuwSaldo = nieuwSaldo;
        this.amount = amount;
    }

    public BalanceLogEntry(UUID speler, PlayerLogType logType, String actie, Bankaccount bankaccount, double oudSaldo, double nieuwSaldo, double amount) {
        super(speler, logType, actie, LocalDateTime.now());

        this.bankaccount = bankaccount;
        this.oudSaldo = oudSaldo;
        this.nieuwSaldo = nieuwSaldo;
        this.amount = amount;
    }



} 