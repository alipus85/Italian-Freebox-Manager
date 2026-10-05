# Refactoring Interfaccia Utente & Download/Torrent Manager - Italian Freebox Manager

Riprogettazione completa dell'interfaccia utente dell'app Android in chiave **Material Design 3**, con accenti cromatici **Rosso Iliad**, navigazione a 4 schede inferiori e **modulo avanzato per la Gestione Torrent e Download** integrato nativamente.

---

> [!IMPORTANT] Scelte di Design & Funzionalità Torrent Confermate
> - **Stile Visivo & Tema**: Tema dinamico Material 3 con tonalità scure eleganti ed accenti Rosso Iliad (`#E53935` / `#D32F2F`).
> - **Struttura Navigazione**: Bottom Navigation Bar a 4 schede fisse (`Home`, `Dispositivi`, `Downloads & Storage`, `Altro`).
> - **Gestione Torrent & Download Manager (In Primo Piano)**:
>   1. Aggiunta rapida di **Magnet Link**, URL HTTP/FTP e caricamento file **`.torrent`**.
>   2. Monitoraggio in tempo reale: velocità di Download/Upload, avanzamento in %, tempo stimato (ETA), numero di Peer/Seeders/Leechers e Ratio.
>   3. Controlli sui task: Avvia, Metti in pausa, Elimina, Priorità e limiti di banda per singolo task.
>   4. Filtri per stato: *In corso*, *Completati*, *In pausa*, *Errori*.

---

## Anteprime Grafiche dell'Interfaccia Utente

### 1. Dashboard Principale (Home)
![Anteprima Dashboard](file:///app/src/main/res/drawable/app_dashboard_mockup_1791124994443.jpg)

### 2. Gestione Torrent e Download Manager
![Anteprima Gestore Torrent e Download](file:///app/src/main/res/drawable/download_manager_mockup_1791125387175.jpg)

---

## 1. Panoramica e Concetto del Gestore Torrent

L'Iliadbox integra un potente **Download Manager nativo** (bittorrent, HTTP, FTP, NZB). La nuova interfaccia trasforma l'app in un **client torrent remoto completo**:

### Funzionalità Chiave della Sezione Downloads
1. **Pulsante Fluttuante (FAB) "+ Nuovo Download"**:
   - Inserimento diretto di **Magnet Link** (con incollamento automatico dalla clipboard).
   - Download da URL (HTTP / HTTPS / FTP).
   - Selezionatore di file per caricare file `.torrent` dalla memoria dello smartphone.
2. **Schede Informative Dettagliate per ogni Torrent**:
   - Barra di avanzamento animata e percentuale.
   - Indicatori in tempo reale di velocità di Download ($\downarrow$ MB/s) ed Upload ($\uparrow$ KB/s).
   - Conteggio Seeders/Leechers, ETA (Tempo rimanente) e Ratio di condivisione.
3. **Azioni Rapide su ciascun Task**:
   - Play / Pausa singoli o cumulativi (*Pausa tutti / Avvia tutti*).
   - Cestino con opzione "Mantieni i file scaricati" oppure "Elimina anche i file".
   - Apertura rapida della cartella di destinazione nell'Esplora File integrato.

---

## 2. Architettura & Struttura Navigazione (4 Schede)

```
┌───────────────────────────────────────────────────────────┐
│                    TOP BAR / STATUS HEADER                │
│  [Iliadbox Status]   [Model Name]   [Refresh / Connect]   │
├───────────────────────────────────────────────────────────┤
│                                                           │
│   TAB 1: HOME (Dashboard & Telemetria)                    │
│   - Meter Velocità Banda (DL/UL) + QR Code Wi-Fi          │
│   - Azioni Rapide (Riavvio, Wi-Fi, Ospiti)                │
│   - Telemetria Sistema (Temp CPU, Storage, Ventola)       │
│                                                           │
│   TAB 2: DISPOSITIVI (Network Hosts & LAN)                │
│   - Lista Hosts con filtri 2.4/5GHz ed Ethernet           │
│                                                           │
│   TAB 3: DOWNLOADS & STORAGE                              │
│   ┌─────────────────────────┬─────────────────────────┐   │
│   │   [★ Torrent & Download]│   [Esplora File Box]    │   │
│   └─────────────────────────┴─────────────────────────┘   │
│   - Stats cumulative: Velocità Totale DL/UL               │
│   - Filtri: Tutti | In Corso | Completati | In Pausa      │
│   - Lista Torrent reattiva con bar di avanzamento         │
│   - [ + ] Floating Action Button (Nuovo Magnet/Torrent)   │
│                                                           │
│   TAB 4: ALTRO (TV, Chiamate, Impostazioni)               │
│   - Guida TV, Registro Chiamate, Permessi e Config        │
│                                                           │
├───────────────────────────────────────────────────────────┤
│  [ Home ]    [ Dispositivi ]   [ Downloads ]    [ Altro ] │
│                BOTTOM NAVIGATION BAR (M3)                 │
└───────────────────────────────────────────────────────────┘
```

---

## 3. Modelli Dati & Componenti Torrent

### Componenti Modulo Torrent
1. **`DownloadManagerScreen.kt`**:
   - `DownloadHeaderCard`: Riepilogo banda usata dai download in tempo reale.
   - `DownloadItemCard`: Card per ciascun file/torrent con progress bar, velocità e comandi.
   - `AddDownloadDialog.kt`: Modale per incollare Magnet Link, URL o caricare `.torrent`.

2. **Dati API Freebox OS coinvolti**:
   - `GET /api/v8/downloads/`: Stato di tutti i task attivi e completati.
   - `POST /api/v8/downloads/add`: Aggiunta nuovo download da URL/Magnet o file base64.
   - `PUT /api/v8/downloads/{id}`: Aggiornamento stato (pausa, ripresa, priorità).
   - `DELETE /api/v8/downloads/{id}`: Eliminazione task/file.

---

## 4. Piano di Esecuzione Passaggio-Passo

1. **Passo 1: Aggiornamento Tema & Colori (`Theme.kt`)**
   - Definire i token M3 con accento Rosso Iliad per Light e Dark mode.

2. **Passo 2: Struttura Base Navigazione a 4 Schede (`MainActivity.kt`)**
   - Implementare `Scaffold` M3 con `NavigationBar` inferiore.

3. **Passo 3: Creazione Dashboard Home (`HomeDashboardScreen.kt`)**
   - Realizzare i widget banda, telemetria, azioni rapide e dialog QR Code Wi-Fi.

4. **Passo 4: Implementazione Sezione Torrent & Downloads (`DownloadsScreen.kt`)**
   - Realizzare la schermata torrent completa di filtri, card interattive e FAB per aggiungere Magnet/URL/Torrent.

5. **Passo 5: Integrazione Esplora File e Sezione Altro**
   - Completare le schermate per la gestione file della box, registro chiamate, TV e permessi router.

6. **Passo 6: Verifica & Compilazione**
   - Compilare con `compile_applet` e verificare l'assenza di warning o errori.
