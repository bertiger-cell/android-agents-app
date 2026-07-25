# Android Agents

Multi-Provider KI-Agent-App fuer Android. Erstelle spezialisierte Agenten und chate mit ihnen ueber OpenRouter, OpenCode Zen oder Ollama (lokal/cloud).

## Features

- Agent-Management mit Room-Datenbank (erstellen, loeschen, Chat-Verlauf)
- Drei Provider: OpenRouter, OpenCode Zen, Ollama Cloud + lokal
- Per-Agent-System-Prompt, Modell und Temperatur
- Ollama-Modell-Auswahl via `/api/tags`
- Einstellungen mit persistenten API-Keys (DataStore)
- Material 3 UI mit Dynamic Color (Android 12+)

## Build

```bash
./gradlew assembleDebug
```

## Stack

Kotlin, Jetpack Compose, Room, DataStore, OkHttp, Navigation Compose
