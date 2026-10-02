# Integrazione Modulo CrealityAuto: OTA GitHub & Versioning APK

Questo piano adotta integralmente l'architettura standard definita nel modulo template di **CrealityAuto**, implementando il versionamento incrementale basato su GitHub Actions, la pubblicazione automatica dell'APK sfuso su GitHub Releases e il modulo nativo di aggiornamento in-app (**OTA AppUpdateManager**) con changelog dei commit.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Specifiche esatte mutuate da CrealityAuto:**
> 1. **Calcolo Versione Dinamico**: In `app/build.gradle.kts`, `versionCode` e `versionName` sono agganciati a `GITHUB_RUN_NUMBER` (es. `1.0.$runNumber` e `100 + runNumber`) con fallback locali se non in CI.
> 2. **Workflow GitHub Actions (`.github/workflows/build-apk.yml`)**:
>    - Compilazione automatica su push `main`/`master` e `workflow_dispatch`.
>    - Ripristino di `debug.keystore.base64` in `$HOME/.android/debug.keystore` con `tr -d '\r\n '` per mantenere firme crittografiche coerenti ad ogni build.
>    - Pubblicazione dell'APK sfuso `ItalianFreeboxManager.apk` sulla release `latest` aggiornando dinamicamente il tag git `latest` e le note con `gh release`.
> 3. **AppUpdateManager.kt**:
>    - Controllo release GitHub (pubbliche e private tramite token facoltativo).
>    - Changelog interattivo dei commit recenti con hash monospace e date formattate.
>    - Download in streaming con percentuale di avanzamento (`%`) e intent di installazione tramite `FileProvider`.
>    - Opzione di reinstallazione / download forzato dell'ultimo APK compilato.
> 4. **Configurazione Risorse e Manifest**:
>    - Authority FileProvider `${applicationId}.fileprovider` con `file_paths.xml` completo (files, cache, external).
>    - Permessi `REQUEST_INSTALL_PACKAGES` e `INTERNET`.
>    - Stringa di default in `strings.xml`: `<string name="default_github_repo">alipus85/ItalianFreeboxManager</string>`.

---

## 1. Overview & Core Concept

- **Cosa fa**: Dà all'applicazione Italian Freebox Manager la piena autonomia di aggiornamento: ogni push su GitHub genera automaticamente l'APK nella release `latest`, e l'app sul telefono rileva l'aggiornamento, mostra il changelog dei commit e scarica/installa l'APK con un tocco.
- **Valore aggiunto**: Replica esattamente l'esperienza fluida e affidabile già collaudata in CrealityAuto.

---

## 2. User Experience & Visual Design

### Scheda Aggiornamenti OTA in `SettingsScreen`

- **Header Versione Attuale**:
  - `ItalianFreebox Manager v1.0.X (Build Y)` con badge di stato colorato.
- **Configurazione Repository**:
  - Mostra il repository associato con valore predefinito `alipus85/ItalianFreeboxManager`.
  - Possibilità di inserire facoltativamente un GitHub Personal Access Token (per repository privati o rate limit GitHub).
- **Pulsante di Verifica**:
  - Pulsante M3 `"Verifica Aggiornamenti OTA"` con animazione di caricamento.
- **Card Stato / Dialog di Aggiornamento**:
  - Mostra la versione remota disponibile rispetto a quella installata.
  - Se è presente un aggiornamento (o su richiesta di reinstallazione forzata):
    - **Lista Changelog Commit**: Elenco degli ultimi commit con hash `abc1234` in stile monospace, messaggio di commit e data.
    - **Barra di Progresso Download**: Barra di caricamento lineare con percentuale (%) e byte scaricati in tempo reale.
    - **Pulsante Azione**: `"Scarica e Installa"` che al termine invoca automaticamente l'installer Android.

---

## 3. Key Product Decisions & Trade-Offs

- **Decisione 1: Tag mobile `latest` per la release diretta dell'APK**
  - *Scelta da CrealityAuto*: La release GitHub usa il tag fisso `latest` che viene fatto avanzare su ogni build via `git tag -f latest HEAD`.
  - *Perché*: L'app può interrogare sempre l'endpoint fisso `/releases/latest` senza dover fare scraping di versioni complesse, e scaricare direttamente l'asset nominato `ItalianFreeboxManager.apk`.

- **Decisione 2: Conservazione della firma tramite `debug.keystore.base64`**
  - *Scelta da CrealityAuto*: Mantenimento del file `debug.keystore.base64` nel repository e decodifica nella pipeline CI.
  - *Perché*: Android rifiuta l'aggiornamento di un'app (errore di installazione pacchetto) se la chiave crittografica con cui è stato firmato l'APK differisce da quella già installata. Usando la stessa chiave di debug, tutti gli aggiornamenti OTA si installano sopra la versione precedente senza richiedere la disinstallazione.

- **Decisione 3: `FileProvider` con authority `${applicationId}.fileprovider`**
  - *Scelta da CrealityAuto*: Unificazione su `${applicationId}.fileprovider` con percorsi comprensivi di cache interna ed esterna.

---

## 4. Technical Architecture & Data Strategy

### Diagramma del Flusso OTA

```
┌────────────────────────────────────────────────────────┐
│             GitHub Actions Workflow                    │
│      .github/workflows/build-apk.yml                   │
│  - Ripristina debug.keystore da debug.keystore.base64  │
│  - gradle :app:assembleDebug                           │
│  - Crea/Aggiorna release 'latest' con                  │
│    ItalianFreeboxManager.apk                           │
└───────────────────────────┬────────────────────────────┘
                            │ Pubblica APK su GitHub Releases
                            ▼
┌────────────────────────────────────────────────────────┐
│                   GitHub API                           │
│   - GET /repos/{repo}/releases/latest                  │
│   - GET /repos/{repo}/commits                          │
└───────────────────────────▲────────────────────────────┘
                            │ Interroga API / Scarica APK
┌───────────────────────────┴────────────────────────────┐
│         com.example.util.AppUpdateManager              │
│  - checkUpdate(repo, token): UpdateResult              │
│  - fetchRecentCommits(repo, token): List<CommitInfo>   │
│  - downloadAndInstallApk(context, apkUrl, onProgress)   │
└───────────────────────────▲────────────────────────────┘
                            │ Notifica stato UI
┌───────────────────────────┴────────────────────────────┐
│          FreeboxViewModel & SettingsScreen             │
│  - Mostra stato controllo, changelog commit            │
│  - Barra di progresso download interattiva             │
│  - Lancio Intent ACTION_VIEW con URI FileProvider      │
└────────────────────────────────────────────────────────┘
```

---

## 5. File da Modificare / Creare

1. **`app/build.gradle.kts`**:
   - Aggiunta del blocco di calcolo dinamico `GITHUB_RUN_NUMBER` e `dynamicVersionCode`.
   - Verifica dipendenza `okhttp` (già presente, verifichiamo la compatibilità).
2. **`.github/workflows/build-apk.yml`**:
   - Creazione del workflow per compilazione automatica, ripristino keystore e pubblicazione su GitHub Releases (`latest`).
   - Rinomina/sostituzione del precedente `android-ci.yml` per evitare workflow duplicati.
3. **`app/src/main/res/values/strings.xml`**:
   - Aggiunta di `<string name="default_github_repo">alipus85/ItalianFreeboxManager</string>`.
4. **`app/src/main/res/xml/file_paths.xml`**:
   - Aggiornamento con tutti i percorsi (internal_files, internal_cache, external_files, external_cache) come da template.
5. **`app/src/main/AndroidManifest.xml`**:
   - Aggiunta del permesso `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`.
   - Aggiornamento authority FileProvider su `${applicationId}.fileprovider`.
6. **`app/src/main/java/com/example/util/AppUpdateManager.kt`**:
   - Implementazione completa del manager OTA con supporto a GitHub Releases, lista commit recente con hash e date, download in background e installazione sicura tramite FileProvider.
7. **`app/src/main/java/com/example/ui/FreeboxViewModel.kt` & `MainActivity.kt`**:
   - Collegamento dello stato degli aggiornamenti nel ViewModel.
   - Creazione della sezione interattiva OTA in `SettingsScreen` con controlli per verificare, visualizzare il changelog commit, e aggiornare/reinstallare l'APK.
8. **Verifica Finale**:
   - Esecuzione di `compile_applet` per confermare che l'intero progetto compili con successo.
