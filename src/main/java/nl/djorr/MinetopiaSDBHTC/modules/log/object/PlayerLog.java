package nl.djorr.MinetopiaSDBHTC.modules.log.object;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.BalanceLogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.LogEntry;
import nl.djorr.MinetopiaSDBHTC.modules.log.type.PlayerLogType;

import java.util.UUID;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class PlayerLog {
    private UUID speler;
    private EnumMap<PlayerLogType, List<LogEntry>> logs = new EnumMap<>(PlayerLogType.class);

    public PlayerLog(UUID speler) {
        this.speler = speler;
        for (PlayerLogType type : PlayerLogType.values()) {
            logs.put(type, new ArrayList<>());
        }
    }
    public List<BalanceLogEntry> getBalanceLogs() {
        List<BalanceLogEntry> result = new ArrayList<>();
        for (LogEntry entry : getLogs(PlayerLogType.BALANCE)) {
            if (entry instanceof BalanceLogEntry) result.add((BalanceLogEntry) entry);
        }
        return result;
    }
    
    public List<BalanceLogEntry> getEssEconomyLogs() {
        List<BalanceLogEntry> result = new ArrayList<>();
        for (LogEntry entry : getLogs(PlayerLogType.ESS_ECONOMY)) {
            if (entry instanceof BalanceLogEntry) result.add((BalanceLogEntry) entry);
        }
        return result;
    }
    public List<LogEntry> getLogs(PlayerLogType type) {
        return logs.getOrDefault(type, new ArrayList<>());
    }
    
    public EnumMap<PlayerLogType, List<LogEntry>> getLogs() {
        return logs;
    }
    
    public UUID getSpeler() {
        return speler;
    }
} 