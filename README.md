# MinetopiaSDB-HTC

Een geavanceerde Minecraft plugin voor Minetopia servers die automatisch alle economische transacties, bank-acties, pickup/drop van wit geld, en andere belangrijke spel-acties logt en bewaart.

## 🚀 Features

### 📊 Uitgebreide Logging
- **Bank Transacties**: Alle stortingen en opnames van alle rekeningtypes
- **Wit Geld Tracking**: Automatische logging van pickup/drop van wit geld items
- **Essentials Economy**: Logging van /eco commando's (give, take, set, reset)
- **Locatie Tracking**: Precieze locaties van alle acties voor onderzoek
- **Tijdstempel**: Gedetailleerde tijdstempels voor alle logs

### 🎮 In-Game Menu Systeem
- **Interactief Menu**: Bekijk logs via een mooi in-game menu
- **Filtering**: Filter logs op type (BALANCE, PICKUP, DROP, ESS_ECONOMY, etc.)
- **Paginering**: Navigeer door grote hoeveelheden logs
- **Teleportatie**: Klik om naar locaties van logs te teleporteren

### 📱 Discord Integratie
- **Real-time Notifications**: Automatische Discord webhooks voor belangrijke acties
- **Rate Limiting**: Slimme rate limiting om Discord API limieten te respecteren
- **Embed Formatting**: Mooie Discord embeds met kleuren per actie type

### 🔧 Automatische Beheer
- **Auto-archivering**: Oude logs worden automatisch gecomprimeerd
- **Retention Policy**: Configureerbare bewaartijd voor logs
- **Backup Systeem**: Automatische backups van corrupte logbestanden

## 📦 Installatie

### Vereisten
- **Minecraft Server**: 1.12.x of hoger
- **Java**: Java 8 of hoger
- **Plugins**:
  - MinetopiaSDB API
  - Essentials (voor economy commando's)
  - NBTEditor (voor wit geld tracking)

### Stappen
1. Download de laatste release van `MinetopiaSDB-HTC.jar`
2. Plaats het bestand in je `plugins/` map
3. Start je server opnieuw op
4. Configureer de plugin via `plugins/MinetopiaSDB-HTC/config.yml`

## ⚙️ Configuratie

### Basis Configuratie
```yaml
# Hoe lang logs zichtbaar blijven in het menu
retention_period: 24h  # Standaard: 24 uur

# Hoe vaak alle logs worden opgeslagen naar disk
save_interval: 5m     # Standaard: elke 5 minuten

# Hoe vaak oude logs worden gecomprimeerd/gearchiveerd
archive_interval: 10m # Standaard: elke 10 minuten

# Discord webhook URL voor logmeldingen
webhook_url: ""
```

### Tijdnotatie
- Gebruik `h` voor uren (bijv. `24h` = 24 uur)
- Gebruik `m` voor minuten (bijv. `5m` = 5 minuten)
- Gebruik `s` voor seconden (bijv. `30s` = 30 seconden)

## 🎯 Commando's

### Voor Spelers
- `/sdbhtc <spelernaam>` - Open het log menu voor een speler

### Voor Staff
- Alle bovenstaande commando's + toegang tot alle logs

## 📁 Bestandsstructuur

```
plugins/MinetopiaSDB-HTC/
├── config.yml                    # Hoofdconfiguratie
├── LOGS/                        # Log bestanden
│   ├── [UUID]/                  # Per speler UUID
│   │   ├── balancelog.json      # Actieve logs
│   │   └── BalanceLOG-archive-*.tar.gz  # Gearchiveerde logs
│   └── ...
└── plugin.yml                   # Plugin metadata
```

## 🔍 Log Types

### BALANCE
- Bank stortingen en opnames
- Alle rekeningtypes (Privé, Spaar, Bedrijf, Overheid)
- Voor- en na-saldo informatie

### PICKUP/DROP
- Wit geld pickup en drop acties
- Item details (type, aantal, waarde)
- Locatie informatie
- Originele eigenaar tracking

### ESS_ECONOMY
- /eco commando's (give, take, set, reset)
- Target speler informatie
- Bedrag wijzigingen

### INVENTORY *TODO*
- Inventory gerelateerde acties
- Creative mode detectie

## 🛠️ Troubleshooting

### Veelvoorkomende Problemen

#### Discord Webhook Errors
```
[MinetopiaSDB-HTC] Fout bij versturen log naar Discord webhook: Server returned HTTP response code: 429
```
**Oplossing**: De plugin heeft automatische rate limiting. Wacht even en de errors stoppen vanzelf.

#### Logs Lijken Weg
**Mogelijke oorzaken**:
1. **Archivering**: Oude logs worden automatisch gearchiveerd
2. **Retention Period**: Controleer `retention_period` in config.yml
3. **Corruptie**: Check voor backup bestanden in de LOGS map

#### Plugin Start Niet
**Controleer**:
- Java versie (minimaal Java 8)
- Afhankelijke plugins zijn geladen
- Permissions zijn correct ingesteld

### Debug Informatie
- Logs worden opgeslagen in `plugins/MinetopiaSDB-HTC/LOGS/`
- Backup bestanden hebben de extensie `.backup.[timestamp]`
- Gearchiveerde logs zijn `.tar.gz` bestanden

## 🔧 Ontwikkeling

### Bouwen
```bash
mvn clean package
```

### Dependencies
- MinetopiaSDB API
- Essentials
- NBTEditor
- Apache Commons Compress
- Lombok

### Code Structuur
```
src/main/java/nl/djorr/MinetopiaSDBHTC/
├── modules/
│   ├── balance/          # Balance tracking
│   ├── config/           # Configuratie management
│   └── log/              # Log systeem
├── command/              # Commando handlers
└── util/                 # Utility classes
```

## 📄 Licentie

Dit project is gelicenseerd onder de **MIT License with Attribution and Non-Commercial Use** - zie het [LICENSE](LICENSE) bestand voor details.

### Licentie Details:
- ✅ **Toegestaan**: Gratis gebruik, forken, aanpassen, hergebruiken
- ❌ **Niet toegestaan**: Verkopen of commercieel gebruik zonder toestemming
- 📝 **Vereist**: Attributie "Original work by Djorr" in alle versies
- 🔄 **Aangemoedigd**: Forking en modificatie van de code

Voor commercieel gebruik, neem contact op met de auteur (Djorr).

## 🤝 Support

Voor vragen, bug reports of feature requests:
- **GitHub Issues**: [Maak een issue aan](https://github.com/djorr/MinetopiaSDB-HTC/issues)
- **Discord**: Neem contact op via Discord

## 🔄 Changelog

### v1.0.0
- Initiële release
- Basis logging functionaliteit
- Discord webhook integratie
- In-game menu systeem
- Auto-archivering

---

**Gemaakt door Djorr** - Voor Minetopia servers wereldwijd! 🎮 