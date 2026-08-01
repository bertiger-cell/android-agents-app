# Roadmap – Agent Studio (android-agents-app)

> **Hinweis:** Diese Roadmap wird aktuell überarbeitet und befindet sich in Bearbeitung.
> Die Details können sich kurzfristig ändern. Diese Version beschreibt den Ist-Stand
> und die aktuelle Planungsrichtung.

---

## Umgesetzt (Stand August 2026)

### Basis & Projekt-Management
- [x] Multi-Provider-Chat (OpenRouter, OpenCode Zen, Ollama Cloud + lokal)
- [x] Streaming-Antworten (SSE/NDJSON)
- [x] Credentials pro Provider (DataStore)
- [x] Dashboard/Welcome-Screen „Agent Studio“
- [x] Projekt erstellen, bearbeiten, löschen (mit Bestätigungsdialog)
- [x] Discovery-Interview wiederholbar
- [x] Edit-Project in Projekt-Detail

### Architect-Flow
- [x] 3-Phasen-Interview (Warm-Up, Interview, Generierung)
- [x] JSON-Extraktion + Validierung + `discovery.json`
- [x] Architect-Summary mit Prompt-Editing
- [x] Generierung von `ARCHITECTURE.md`, `ROADMAP.md`, `RULES.md`, `SKILLS.md`, `AGENTS.md`, `diary.md`
- [x] Automatische Agenten-Erstellung aus `suggested_agents`
- [x] Architect Provider/Modell/System-Prompt in Settings konfigurierbar

### App-Features
- [x] Dateien-Tab im Projekt (Viewer für `.md` + `.json`, Metadaten, Refresh)
- [x] Loading-Overlay + Toast bei Datei-Generierung
- [x] Chat mit Auto-Scroll, Session-Rename, Titel-Generierung
- [x] Delete-Confirmation für Projekte und Agenten

### Phase 2 (V2.0) – umgesetzt
- [x] Projekt-Export/Import als ZIP (Phase 2a) inkl. `manifest.json` und Attachments
- [x] Session-Löschung einzeln im UI (mit Bestätigung)
- [x] Agent-Templates: persistente Vorlagen via DataStore (speichern/laden/löschen)
- [x] Projekt-Ordnerstruktur erweitert: `media/`, `audio/`, `exports/`
- [x] Chat-Attachments: Bilder/Dateien anhängen, Vorschau, Datei-Chips
- [x] Error-Boundary: zentraler Fehler-State + Snackbar-Feedback
- [x] Modulare Architektur: Feature-Pakete (`feature/projects`, `chat`, `agents`, `settings`)
- [x] 10 Unit-Test-Suiten (53 Tests)

---

## In Arbeit / Festeigung

- [x] Doku auf aktuellen Repo-Stand bringen (ARCHITECTURE/ROADMAP/CHANGELOG)
- [ ] Performance-Verhalten bei sehr großen Sessions beobachten
- [ ] Weitere Tests für Repository-/ViewModel-Logik

---

## Geplant / Richtung (Details folgen)

- **LangChain4j + ToolRegistry** – Agent-Framework mit registrierbaren Tools (Non-Goal V2)
- **On-Device-ML** – LiteRT (LLM-Inference) + MediaPipe (Vision/Audio) (Non-Goal V2)
- **Vision** – Bilder analysieren (Kamera/Galerie) (Non-Goal V2)
- **Bildgenerierung** – remote/on-device, speichern im Projekt-Ordner (Non-Goal V2)
- **Bild-Skalierung/-Bearbeitung**
- **Speech-to-Text / Text-to-Speech** (Non-Goal V2)

---

## Aktuelle Non-Goals

- Multi-Agent-Orchestrierung / Debating
- RAG / Vector Store
- Geplante Automatisierung (WorkManager)
- Termux-Hub

Diese Punkte sind nicht grundsätzlich ausgeschlossen, werden aber erst nach
der ToolRegistry- und Modul-Phase priorisiert.
