# Dirigenti Quiz

App Android nativa per esercitarsi con la banca dati della prova preselettiva del concorso per dirigenti scolastici.

## Funzionalità

- Carica `3.681` quesiti estratti dal PDF `Domande_Prova-Preselettiva_DS.pdf`.
- Permette di esercitarsi su tutte le domande oppure su una singola area tematica.
- Mischia l'ordine delle risposte: nel PDF la risposta corretta è sempre `[a]`, ma nell'app non resta sempre nella stessa posizione.
- Mostra punteggio, numero di risposte corrette/errate e percentuale di preparazione.
- Funziona offline perché la banca dati è inclusa negli asset dell'app.

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
