# AGENTS.md

## Projekt
Android-App (Kotlin/Jetpack Compose), Multi-Provider AI-Agent
(OpenRouter, OpenCode Zen, Ollama) mit Streaming-Chat,
Agent-Vorlagen, Model-Picker und animiertem Intro-Screen.

Volle Architektur: siehe `ARCHITECTURE.md`.
Detaillierte Arbeitsregeln: siehe `RULES.md`.
Fahrplan: siehe `ROADMAP.md`.

## Agenten-Rollen

### Coder-Agent (Code-Generierung & Umsetzung)
Zuständig für die Generierung und Implementierung von Code.
Verfügt über folgende Skills:

- **kotlin-idioms-refactor**: Analysiert bestehenden Code und wandelt ihn in hoch-idiomatischen Kotlin-Code um. Nutzt Erweiterungen, Scoping-Funktionen (`let`, `apply`, `also`, `run`), Data Classes, Sealed Classes/Interfaces und Pattern Matching via `when`.
- **android-architecture-builder**: Generiert und strukturiert App-Architekturen nach Google-Empfehlungen. Erstellt Clean-Architecture-Schichten, setzt MVVM/MVI um, implementiert das Repository Pattern und richtet DI via Hilt oder Koin ein.
- **kotlin-coroutines-flow**: Entwickelt und optimiert asynchrone Abläufe und Datenstreams. StateFlow, SharedFlow, benutzerdefinierte Operatoren, Coroutine-Contexts, Error-Handling und Structured Concurrency.
- **gradle-kts-migration**: Verwaltet und modernisiert das Build-System. Konvertierung zu Kotlin DSL, Version Catalogs, Multi-Modul-Projekte.
- **compose-ui-generator**: Erstellt moderne deklarative UIs in Jetpack Compose. `@Composable`-Funktionen, Layout-Composables, State Hoisting, `@Preview`.
- **material3-theme-generator**: Implementiert Material Design 3. Dynamische Farbschemata (inkl. Dynamic Color), Dark/Light-Themes, Typografie/Shape/Surface.
- **room-database-engineer**: Erstellt und verwaltet die lokale Persistenz mit Room. `@Entity`, `@Dao`, TypeConverter, SQL-Queries, Migrationen, FTS.
- **android-local-ai-integration**: Einbindung von On-Device-KI (LiteRT, MediaPipe). JNI/C++-Bindings, Speicheroptimierung, lokale Prompts/Inferenzen.
- **android-background-tasks**: Verwaltet Hintergrundprozesse. WorkManager, Foreground Services, Doze-Mode, App-Standby.

### Architekt-Agent (Kontrolle & Qualitätssicherung)
Zuständig für Prüfung, Optimierung und Qualitätssicherung.
Verfügt über folgende analytische Skills:

- **compose-performance-optimizer**: Identifiziert und behebt Performance-Flaschenhälse in Compose UI-Trees. Analysiert Recomposition, `@Stable`/`@Immutable`, `derivedStateOf`, LazyList-Keys.
- **github-actions-android-ci**: Automatisiert Build/Test/Deployment. Workflow-YML, APK/AAB-Builds, Caching, Signieren von Release-Builds.
- **github-pr-assistant**: Unterstützt Code-Review auf GitHub. PR-Beschreibungen, Git-Diff-Prüfung, semantische Commits.
- **compose-navigation-manager**: Verwaltet Navigation Compose typsicher. Argument-Pässe, Navigations-Graphen, ViewModel-Lifecycle-Anbindung.

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

## Architect Agent Skill (v3)

Siehe SKILLS_ARCHITECT.md für die vollständige Dokumentation.

Der Architect Agent ist KEIN permanenter Agent, sondern ein Discovery-Workflow:
- Automatischer Start bei neuer Projekt-Erstellung
- 3-Phasen-Interview (Warm-Up, Interview, Generierung)
- Generiert ARCHITECTURE.md, ROADMAP.md, RULES.md, AGENTS.md
- Erstellt spezialisierte Agenten aus suggested_agents
- Edtierbarer System Prompt in den Settings
