# AGENTS.md

## Projekt
Android-App (Kotlin/Jetpack Compose), Multi-Provider AI-Agent
(OpenRouter, OpenCode Zen, Ollama) mit Streaming-Chat,
Agent-Vorlagen, Model-Picker und animiertem Intro-Screen.

Volle Architektur: siehe `ARCHITECTURE.md`.
Detaillierte Arbeitsregeln: siehe `RULES.md`.
Fahrplan: siehe `ROADMAP.md`.

## Build & Verifikation
- Build: `./gradlew assembleDebug`
- Test: `./gradlew test`
- Lint: `./gradlew lint`
- Eine Änderung gilt erst als "getestet", wenn einer dieser Befehle
  tatsächlich ausgeführt wurde – nicht annehmen, nicht behaupten.

## Wichtigste Regeln (Kurzfassung – Details in RULES.md)
- Nur die explizit angeforderte(n) Datei(en) ändern. Keine
  Nebenrefactorings, keine "während ich dabei war"-Änderungen.
- Keine neuen Dependencies in `build.gradle.kts` ohne Rückfrage.
- `ARCHITECTURE.md` ist bindend. Die v1-Non-Goals werden nicht
  vorab oder als Stub eingebaut.
- Bei Mehrdeutigkeit im Auftrag: fragen, nicht raten.
- Max. 2 Dateien pro Aufgabe ohne Rückfrage (Regel 9).
- Build nach JEDER Datei-Änderung (Regel 8).

## Verifikations-Check (für Robert)
Gelegentlich prüfen, ob die Regeln wirklich geladen wurden mit:
"Fasse die aktuell geltenden Projektregeln zusammen." Die Antwort
sollte Build-Befehle und die Kernregeln oben nennen.
