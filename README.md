# Italian Freebox Manager

Un'applicazione Android moderna per la gestione del tuo router Freebox/Iliadbox, sviluppata interamente in Kotlin utilizzando Jetpack Compose.

## Caratteristiche Principali

- **📁 Gestione File su Hard Disk (Evidenziata):** Navigazione completa, visualizzazione e gestione dei file archiviati direttamente sull'Hard Disk del tuo Freebox/Iliadbox. Supporta l'upload sicuro e l'organizzazione flessibile delle tue cartelle.
- **⚡ Download Manager (Torrent & Link Magnet):** Controllo totale e monitoraggio in tempo reale dei tuoi download. Gestisci, avvia, metti in pausa o elimina i file torrent e i download attivi direttamente dall'applicazione in mobilità.
- **Gestione API:** Interazione diretta con le API Freebox OS per il controllo remoto e locale del router.
- **Interfaccia Utente:** Design pulito e moderno basato su Material Design 3, ottimizzato per un'esperienza fluida.
- **Gestione Dispositivi:** Visualizzazione in tempo reale dei dispositivi connessi alla rete.
- **Monitoraggio Hardware:** Controllo delle prestazioni del sistema, inclusa temperatura CPU, velocità della ventola e tempo di attività.
- **Controllo Sistema:** Funzionalità dedicata per il riavvio del dispositivo direttamente dall'app.

## Requisiti

- Android SDK (API 24 o superiore)
- Kotlin (supportato via Gradle)
- Connessione alla rete del router Freebox/Iliadbox per la configurazione iniziale

## Installazione e Build

> [!IMPORTANT]
> **Nota di Test/Debug:** Questa applicazione è attualmente configurata ed erogata come **versione di test/debug** (Debug Build). Questo consente di effettuare test rapidi sul dispositivo senza necessità di firme di produzione (Release keys), ed è ideale per scopi di sviluppo, monitoraggio e debug diretto.

### Utilizzo del Workflow GitHub (Consigliato)
Puoi scaricare l'ultima versione compilata dell'APK direttamente da GitHub:
1. Vai alla sezione **Actions** di questo repository.
2. Seleziona il workflow "Android CI".
3. Scarica l'artefatto dall'ultima esecuzione riuscita. L'APK generato sarà rinominato automaticamente in **`Italianfreeboxmanager.apk`**.

### Compilazione Locale
1. Clona questo repository sul tuo computer.
2. Apri il progetto in Android Studio o utilizza il terminale.
3. Per compilare la versione di debug/test, esegui il comando:
   ```bash
   ./gradlew assembleDebug
   ```
4. L'APK generato (**`Italianfreeboxmanager.apk`**) si troverà nella directory:
   `app/build/outputs/apk/debug/`

## Licenza

Questo progetto è distribuito a scopo dimostrativo.
