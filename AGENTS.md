# AGENTS.md

## Projekt
Android-App (Kotlin/Jetpack Compose), Multi-Provider AI-Agent
(OpenAI, Anthropic, Ollama). Aktuell v1: einfacher Chat, ein Provider
gleichzeitig auswählbar.

Volle Architektur: siehe `ARCHITECTURE.md`.
Detaillierte Arbeitsregeln: siehe `RULES.md`.

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
- `ARCHITECTURE.md` ist bindend. Die v1-Non-Goals (Tool-Calling,
  Multi-Agenten-Orchestrierung, On-Device-Inferenz, RAG, Streaming)
  werden nicht vorab oder als Stub eingebaut.
- Bei Mehrdeutigkeit im Auftrag: fragen, nicht raten.

## Verifikations-Check (für Robert)
Gelegentlich prüfen, ob die Regeln wirklich geladen wurden, mit:
"Fasse die aktuell geltenden Projektregeln zusammen." Die Antwort
sollte Build-Befehle und die Kernregeln oben nennen.
