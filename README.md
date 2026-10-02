# Italian Freebox Manager

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/OTA%20Updates-GitHub%20Releases-24292e?style=for-the-badge&logo=github&logoColor=white" alt="OTA Updates" />
  <img src="https://img.shields.io/badge/Router-Freebox%20%7C%20Iliadbox-E2001A?style=for-the-badge" alt="Freebox / Iliadbox" />
</p>

Un'applicazione Android moderna, fluida e completa per la gestione dei router **Freebox** e **Iliadbox**, sviluppata interamente in Kotlin con **Jetpack Compose** e **Material Design 3**.

---

## 📑 Indice

1. [Funzionalità Principali](#-funzionalità-principali)
2. [Aggiornamenti OTA In-App & Versioning](#-aggiornamenti-ota-in-app--versioning)
3. [Primo Avvio & Procedura di Accoppiamento](#-primo-avvio--procedura-di-accoppiamento)
4. [Pipeline CI/CD GitHub Actions](#-pipeline-cicd-github-actions)
5. [Guida per Sviluppatori & Template OTA](#-guida-per-sviluppatori--template-ota)
6. [Download & Compilazione](#-download--compilazione)
7. [Requisiti e Note](#-requisiti-e-note)

---

## 🚀 Funzionalità Principali

### 🔄 Aggiornamenti OTA In-App (GitHub Releases)
- **Verifica Diretta:** Controllo delle nuove release e compilazioni senza necessità di accedere al browser.
- **Changelog Commit Interattivo:** Elenco cronologico degli ultimi commit con hash breve monospace, autore, data e messaggio.
- **Download con Progresso:** Notifica visiva della percentuale e dei MB scaricati in tempo reale.
- **Installazione Automatica:** Avvio dell'installer Android nativo tramite `FileProvider` con un solo tocco.
- **Reinstallazione Facile:** Possibilità di riscaricare e reinstallare l'ultimo APK compilato in qualsiasi momento.

### 📁 File Manager dell'Hard Disk Router
- Esplorazione completa di dischi interni, dischi SATA e memorie USB collegate al router.
- Navigazione ad albero, creazione nuove directory e caricamento file.
- Eliminazione di file e cartelle con **auto-refresh immediato e sincronizzato**.
- Download diretto dei file sul dispositivo mobile.

### ⚡ Torrent & Download Manager
- Gestione in tempo reale dei download sul router (Torrent, Magnet Link, HTTP, FTP).
- Monitoraggio dettagliato di **Seed**, **Peer**, velocità di upload/download ed ETA calcolato.
- Integrazione nativa delle API avanzate Freebox OS:
  - `GET /api/v8/downloads/{id}/peers`
  - `GET /api/v8/downloads/{id}/trackers`
- Controlli per avviare, sospendere, riprendere o cancellare download e ripulire le code completate.

### 💻 Dispositivi di Rete & LAN
- Mappa dei dispositivi connessi via cavo Ethernet o rete Wi-Fi.
- Informazioni dettagliate per ciascun host: nome, IP locale, MAC address, vendor e stato online/offline.

### ⚙️ Monitoraggio Sistema & Controlli Rapidi
- Temperatura CPU, velocità della ventola in RPM e tempo di attività continuo (*uptime*).
- Visualizzazione dello stato e della tipologia di connessione (Fibra 1G / 2.5G / 5G / 10G EPON).
- Pulsante per il **riavvio immediato del router** con conferma di sicurezza.

---

## 🔄 Aggiornamenti OTA In-App & Versioning

L'applicazione include il modulo nativo **`AppUpdateManager`** che permette di mantenere l'app sempre aggiornata all'ultima versione direttamente dal dispositivo:

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Releases                      │
│        (https://api.github.com/repos/.../releases)     │
└───────────────────────────▲────────────────────────────┘
                            │ GET /releases/latest & /commits
                            │
┌───────────────────────────┴────────────────────────────┐
│         com.example.util.AppUpdateManager              │
│  - Controllo release e changelog ultimi commit         │
│  - Download streaming con callback avanzamento (%)     │
│  - Installazione nativa via FileProvider               │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│      SettingsScreen -> Scheda AGGIORNAMENTI OTA        │
│  - Badge di stato (Aggiornata / Nuova v1.0.X)          │
│  - Repository configurabile: alipus85/Italian-Freebox..│
│  - Pulsanti: "Verifica OTA", "Scarica e Installa"      │
└────────────────────────────────────────────────────────┘
```

### Configurazione nel file `app/build.gradle.kts`:
Il numero di versione e il build code sono incrementati automaticamente dalla pipeline GitHub:
```kotlin
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val dynamicVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: (100 + runNumber)

defaultConfig {
    applicationId = "com.alipus85.italianfreeboxmanager"
    minSdk = 24
    targetSdk = 34
    versionCode = dynamicVersionCode
    versionName = "1.0.$runNumber"
}
```

---

## 🔐 Primo Avvio & Procedura di Accoppiamento

1. **Connessione alla rete:** Collega il tuo smartphone alla rete Wi-Fi o locale del tuo router Freebox/Iliadbox.
2. **Indirizzo del router:** L'applicazione è preconfigurata sull'endpoint `http://myiliadbox.iliad.it/` (oppure `http://mafreebox.freebox.fr/`). Puoi anche specificare un indirizzo DNS/IP personalizzato per l'accesso remoto.
3. **Associazione (Handshake):**
   - Premi **"Registra App"** nella schermata delle Impostazioni o nella notifica iniziale.
   - Guarda lo **schermo frontale del tuo router Freebox / Iliadbox** e premi la freccia destra o tocca il display per **confermare la richiesta di accesso**.
   - L'app riceverà il proprio `app_token` permanente crittografato e genererà la sessione sicura tramite HMAC-SHA1.

---

## 🛠 Pipeline CI/CD GitHub Actions

Il file `.github/workflows/build-apk.yml` gestisce l'intero ciclo di build e rilascio:

1. **Trigger:** Si attiva ad ogni `push` sui rami `main` e `master` o manualmente via `workflow_dispatch`.
2. **Keystore Deterministico:** Ripristina `debug.keystore` partendo dal file `debug.keystore.base64` presente nel repository. Questo assicura che **la chiave di firma rimanga sempre identica tra un aggiornamento e l'altro**, evitando l'errore Android di *firma non concordante* durante gli aggiornamenti OTA.
3. **Build:** Esegue `gradle :app:assembleDebug --no-daemon`.
4. **Artefatto:** Prepara e carica `ItalianFreeboxManager.apk` negli artifact del run.
5. **Release `latest`:** Muove il tag Git `latest` sull'ultimo commit e aggiorna la release GitHub allegando il file `ItalianFreeboxManager.apk`.

---

## 💡 Guida per Sviluppatori & Template OTA

Se vuoi replicare questo stesso sistema di **aggiornamenti OTA e versionamento automatico** su altri progetti Android:

1. **Aggiungi `debug.keystore.base64`** nella root del progetto.
2. **Copia `.github/workflows/build-apk.yml`** personalizzando il nome dell'APK e il repository.
3. **Configura `app/build.gradle.kts`** con `GITHUB_RUN_NUMBER`.
4. **Copia `AppUpdateManager.kt`** nella cartella `util` del tuo progetto.
5. **Dichiara il `FileProvider`** in `AndroidManifest.xml` con authority `${applicationId}.fileprovider` e crea `res/xml/file_paths.xml`.
6. **Aggiungi il permesso** `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`.
7. **Definisci la risorsa stringa** `<string name="default_github_repo">tuo-utente/tuo-progetto</string>`.

---

## 📲 Download & Compilazione

### 1. Download Diretto da GitHub Releases (Raccomandato per utenti)
Scarica l'ultimo file APK compilato dalla pagina [GitHub Releases](../../releases):
- Scarica **`ItalianFreeboxManager.apk`**
- Apri il file e conferma l'installazione.

### 2. Compilazione Locale con Gradle
Se preferisci compilare il codice sorgente:
```bash
# Clona il repository
git clone https://github.com/alipus85/Italian-Freebox-Manager.git
cd Italian-Freebox-Manager

# Compila l'APK di debug
./gradlew assembleDebug

# L'APK compilato si trova in:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Requisiti e Note

- **Android:** Versione 7.0 (Nougat, API 24) o superiore.
- **Router Compatibili:**
  - Iliadbox Wi-Fi 6 / Wi-Fi 7 (Italia)
  - Freebox Revolution, Mini 4K, One, Delta, Pop, Ultra (Francia/Italia)
- **API Freebox OS:** Compatibile con le specifiche da v3 fino a v15+.
- **Installazione APK Sconosciuti:** Su Android 8.0+ è necessario abilitare l'autorizzazione all'installazione di pacchetti per Italian Freebox Manager al primo aggiornamento OTA.

---

## 📄 Licenza

Progetto open-source rilasciato per finalità di gestione personale e monitoraggio del router Freebox/Iliadbox.
