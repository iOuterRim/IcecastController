# 📻 Icecast Controller – Android App

Eine Android-App, die als **Cast-Controller** fungiert: Sie empfängt einen Icecast-Audiostream und überträgt ihn direkt auf ein **Google Cast-fähiges Gerät** (z.B. JBL Authentics 200, Chromecast Audio, Google Home).

---

## Funktionen

- **Quellenauswahl** – Beliebig viele Icecast/HTTP-Streams speichern und auswählen
- **Cast-Empfänger-Auswahl** – Standard Google Cast Dialog (alle Cast-Geräte im WLAN)
- **Play / Pause / Stop** – Vollständige Wiedergabesteuerung
- **Lautstärke-Regelung** – Seekbar + ±5%-Buttons, direktes Setzen der Cast-Gerätelautstärke
- **Persistenz** – Quellen werden lokal gespeichert (SharedPreferences + Gson)
- **Status-Anzeige** – Verbinden, Buffert, Spielt, Pausiert

---

## Voraussetzungen

- Android Studio **Hedgehog (2023.1.1)** oder neuer
- Android SDK 26+
- Ein Google-Cast-fähiges Gerät im selben WLAN (z.B. JBL Authentics 200)
- **Wichtig:** Das Gerät muss Google Play Services haben (kein Emulator für Cast-Tests!)

---

## Einrichtung

### 1. Projekt öffnen

```
File → Open → IcecastController Ordner auswählen
```

Gradle sync automatisch abwarten.

### 2. Cast App ID (optional)

Die App nutzt den **Default Media Receiver** von Google – das bedeutet:
- **Keine Registrierung** bei der Google Cast Developer Console nötig
- Funktioniert sofort auf allen Cast-Geräten

Wenn du einen eigenen Styled Receiver bauen willst (eigenes Logo/UI auf dem TV), musst du:
1. Auf https://cast.google.com/publish registrieren
2. Eigene App-ID in `CastOptionsProvider.kt` eintragen:
   ```kotlin
   .setReceiverApplicationId("DEINE_APP_ID")
   ```

### 3. App auf Gerät deployen

```
Run → Run 'app' → Dein physisches Android-Gerät auswählen
```

---

## Projektstruktur

```
app/src/main/java/com/example/icecastcontroller/
│
├── CastOptionsProvider.kt      # Google Cast Konfiguration
├── MainActivity.kt             # Haupt-Controller UI + Cast-Logik
├── AddSourceActivity.kt        # Dialog zum Hinzufügen von Quellen
├── MainViewModel.kt            # State Management (LiveData)
├── SourceAdapter.kt            # RecyclerView Adapter für Quellen-Liste
├── StreamSource.kt             # Datenmodell für eine Stream-Quelle
└── StreamSourceRepository.kt  # Persistenz (SharedPreferences + Gson)
```

---

## Benutzung

### Stream hinzufügen

1. Tippe auf den **+** Button (unten rechts)
2. Name und URL eingeben (z.B. `http://mein-server:8000/stream`)
3. MIME-Type auswählen (wird automatisch erkannt, Standard: `audio/mpeg`)
4. **Speichern**

### Mit Cast-Gerät verbinden

1. Tippe auf das **Cast-Symbol** in der Toolbar (oben rechts)
2. Wähle dein Gerät aus (z.B. "JBL Authentics 200")

### Stream abspielen

1. Tippe auf eine Quelle in der Liste → wird blau markiert
2. Tippe **▶ Abspielen**
3. Das Cast-Gerät holt den Stream direkt vom Server

---

## HTTPS-Hinweis

Viele Cast-Geräte lehnen **HTTP**-Streams ab (nur HTTPS erlaubt in modernen Firmwares).

**Lösung mit Nginx als HTTPS-Proxy:**

```nginx
server {
    listen 443 ssl;
    server_name dein-domain.de;

    ssl_certificate /etc/letsencrypt/live/dein-domain.de/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dein-domain.de/privkey.pem;

    location /stream {
        proxy_pass http://localhost:8000/stream;
        proxy_set_header Host $host;
        proxy_buffering off;
    }
}
```

---

## MIME-Types

| Format    | MIME-Type        | Cast-Kompatibilität |
|-----------|------------------|---------------------|
| MP3       | `audio/mpeg`     | ✅ Sehr gut         |
| AAC       | `audio/aac`      | ✅ Gut              |
| Ogg       | `audio/ogg`      | ⚠️ Eingeschränkt    |
| FLAC      | `audio/flac`     | ⚠️ Geräteabhängig   |

---

## Abhängigkeiten

```gradle
com.google.android.gms:play-services-cast-framework:21.4.0
androidx.mediarouter:mediarouter:1.7.0
com.google.code.gson:gson:2.10.1
androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
```

---

## Erweiterungsideen

- [ ] Icecast-Metadaten (Titel, Artist) vom Server lesen und anzeigen
- [ ] Favoriten / Reihenfolge per Drag & Drop ändern
- [ ] Hintergrund-Service für persistente Verbindung
- [ ] Widget für die Statusleiste
- [ ] Unterstützung für M3U/PLS Playlisten-Formate
- [ ] Dark Mode
