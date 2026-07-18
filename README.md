# Dirigenti Quiz

App Android nativa per esercitarsi con la banca dati della prova preselettiva del concorso per dirigenti scolastici.

## Funzionalità

- Carica `3.955` quesiti puliti estratti dal PDF `Domande_Prova-Preselettiva_DS.pdf`.
- Permette di esercitarsi su tutte le domande oppure su una singola area tematica con nome leggibile.
- Mischia l'ordine delle risposte: nel PDF la risposta corretta è sempre `[a]`, ma nell'app non resta sempre nella stessa posizione.
- Mostra punteggio, numero di risposte corrette/errate e percentuale di preparazione.
- Dopo ogni risposta mostra il pulsante **Spiegazione** con una mini spiegazione semplice della risposta corretta.
- Presenta una schermata iniziale pulita per scegliere l'area prima di iniziare l'allenamento.
- Funziona offline perché la banca dati è inclusa negli asset dell'app.

## Aree tematiche incluse

1. Normativa del sistema educativo
2. Conduzione delle organizzazioni complesse
3. Programmazione, gestione e valutazione scolastica
4. Ambienti di apprendimento, inclusione e digitale
5. Organizzazione del lavoro e gestione del personale
6. Valutazione e autovalutazione delle scuole
7. Diritto civile e amministrativo
8. Contabilità di Stato e gestione finanziaria
9. Sistemi educativi europei

## Dove scaricare l'APK funzionante

L'APK viene generato automaticamente da GitHub Actions:

1. Apri la pagina del repository su GitHub.
2. Vai nella scheda **Actions**.
3. Apri l'ultima esecuzione del workflow **Build Android APK** sul branch di questa modifica.
4. In fondo alla pagina, nella sezione **Artifacts**, scarica **dirigenti-quiz-debug-apk**.
5. Estrai lo ZIP scaricato: dentro trovi `app-debug.apk`.

## Installazione su Android

### Installazione direttamente dal telefono

1. Copia `app-debug.apk` sul telefono oppure scaricalo dal browser del telefono.
2. Tocca il file APK.
3. Se Android lo richiede, abilita **Installa app sconosciute** per il browser/file manager usato.
4. Conferma **Installa**.

### Installazione con ADB dal computer

```bash
adb install -r app-debug.apk
```

L'APK debug generato è installabile su Android 6.0+ (`minSdk 23`).

## Build locale alternativa

In un ambiente con Android Gradle Plugin e Android SDK disponibili:

```bash
gradle :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Rigenerare la banca dati

```bash
python3 tools/extract_questions.py
```

Lo script legge il PDF nella root del repository e rigenera `app/src/main/assets/questions.json`.
