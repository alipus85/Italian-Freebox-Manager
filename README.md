# Italian Freebox Manager

Un'applicazione Android moderna per la gestione del tuo router Freebox/Iliadbox, sviluppata interamente in Kotlin utilizzando Jetpack Compose.

## Caratteristiche Principali

- **Gestione API:** Interazione diretta con le API Freebox OS per il controllo remoto e locale del router.
- **Interfaccia Utente:** Design pulito e moderno basato su Material Design 3, ottimizzato per un'esperienza fluida.
- **Gestione Dispositivi:** Visualizzazione in tempo reale dei dispositivi connessi alla rete.
- **Monitoraggio Hardware:** Controllo delle prestazioni del sistema, inclusa temperatura CPU, velocità della ventola e tempo di attività.
- **Gestione File:** Accesso integrato ai file archiviati sul server Freebox.
- **Controllo Sistema:** Funzionalità dedicata per il riavvio del dispositivo direttamente dall'app.

## Requisiti

- Android SDK (API 24 o superiore)
- Kotlin (supportato via Gradle)
- Connessione alla rete del router Freebox/Iliadbox per la configurazione iniziale

## Installazione e Build

### Utilizzo del Workflow GitHub (Consigliato)
Puoi scaricare l'ultima versione compilata dell'APK direttamente da GitHub:
1. Vai alla sezione **Actions** di questo repository.
2. Seleziona il workflow "Android CI".
3. Scarica l'artefatto denominato `ItalianFreeboxManager` dall'ultima esecuzione riuscita.

### Compilazione Locale
1. Clona questo repository sul tuo computer.
2. Apri il progetto in Android Studio o utilizza il terminale.
3. Per compilare il progetto, esegui il comando:
   ```bash
   ./gradlew assembleDebug
   ```
4. L'APK generato si troverà in `app/build/outputs/apk/debug/`.

## Licenza

Questo progetto è distribuito a scopo dimostrativo.
