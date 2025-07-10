package nl.djorr.MinetopiaSDBHTC.modules.log.type;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import nl.minetopiasdb.api.banking.Bankaccount;
import org.bukkit.Location;
import com.google.gson.*;
import java.lang.reflect.Type;

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
    /**
     * Bij pickup/drop logs: de originele eigenaar (UUID) van het item.
     * Bij eco logs: de target speler.
     */
    private UUID target;

    public BalanceLogEntry() {
        super();
    }

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

    // Custom serializer for BalanceLogEntry
    public static class Serializer implements JsonSerializer<BalanceLogEntry> {
        @Override
        public JsonElement serialize(BalanceLogEntry src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("speler", src.getSpeler() != null ? src.getSpeler().toString() : null);
            obj.addProperty("logType", src.getLogType() != null ? src.getLogType().name() : null);
            obj.addProperty("actie", src.getActie());
            obj.add("timestamp", context.serialize(src.getTimestamp()));
            if (src.getOudSaldo() != 0.0) obj.addProperty("oudSaldo", src.getOudSaldo());
            if (src.getNieuwSaldo() != 0.0) obj.addProperty("nieuwSaldo", src.getNieuwSaldo());
            if (src.getAmount() != 0.0) obj.addProperty("amount", src.getAmount());
            if (src.getItemAmount() != 0) obj.addProperty("itemAmount", src.getItemAmount());
            if (src.getItemType() != null) obj.addProperty("itemType", src.getItemType());
            if (src.getLocation() != null) obj.add("location", context.serialize(src.getLocation()));
            if (src.getTarget() != null) obj.addProperty("target", src.getTarget().toString());
            // Bankaccount wordt apart geserialiseerd in LogModule indien nodig
            return obj;
        }
    }

    // Custom deserializer for BalanceLogEntry
    public static class Deserializer implements JsonDeserializer<BalanceLogEntry> {
        @Override
        public BalanceLogEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            UUID speler = obj.has("speler") && !obj.get("speler").isJsonNull() ? UUID.fromString(obj.get("speler").getAsString()) : null;
            PlayerLogType logType = obj.has("logType") && !obj.get("logType").isJsonNull() ? PlayerLogType.valueOf(obj.get("logType").getAsString()) : null;
            String actie = obj.has("actie") && !obj.get("actie").isJsonNull() ? obj.get("actie").getAsString() : null;
            LocalDateTime timestamp = obj.has("timestamp") && !obj.get("timestamp").isJsonNull() ? context.deserialize(obj.get("timestamp"), LocalDateTime.class) : LocalDateTime.now();
            double oudSaldo = obj.has("oudSaldo") && !obj.get("oudSaldo").isJsonNull() ? obj.get("oudSaldo").getAsDouble() : 0.0;
            double nieuwSaldo = obj.has("nieuwSaldo") && !obj.get("nieuwSaldo").isJsonNull() ? obj.get("nieuwSaldo").getAsDouble() : 0.0;
            double amount = obj.has("amount") && !obj.get("amount").isJsonNull() ? obj.get("amount").getAsDouble() : 0.0;
            int itemAmount = obj.has("itemAmount") && !obj.get("itemAmount").isJsonNull() ? obj.get("itemAmount").getAsInt() : 0;
            String itemType = obj.has("itemType") && !obj.get("itemType").isJsonNull() ? obj.get("itemType").getAsString() : null;
            Location location = obj.has("location") && !obj.get("location").isJsonNull() ? context.deserialize(obj.get("location"), Location.class) : null;
            UUID target = obj.has("target") && !obj.get("target").isJsonNull() ? UUID.fromString(obj.get("target").getAsString()) : null;
            // Bankaccount wordt apart geladen in LogModule indien nodig

            BalanceLogEntry entry = new BalanceLogEntry();
            entry.setSpeler(speler);
            entry.setLogType(logType);
            entry.setActie(actie);
            entry.setTimestamp(timestamp);
            entry.setOudSaldo(oudSaldo);
            entry.setNieuwSaldo(nieuwSaldo);
            entry.setAmount(amount);
            entry.setItemAmount(itemAmount);
            entry.setItemType(itemType);
            entry.setLocation(location);
            entry.setTarget(target);
            return entry;
        }
    }
} 