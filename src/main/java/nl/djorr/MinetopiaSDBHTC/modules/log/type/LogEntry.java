package nl.djorr.MinetopiaSDBHTC.modules.log.type;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class LogEntry {
    private UUID speler;
    private PlayerLogType logType;
    private String actie;
    private LocalDateTime timestamp;
} 