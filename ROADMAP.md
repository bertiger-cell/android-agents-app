# Roadmap – Agent Studio (android-agents-app)

> **Hinweis:** Diese Roadmap wird aktuell überarbeitet und befindet sich in Bearbeitung.
> Die Details können sich kurzfristig ändern. Diese Version beschreibt den Ist-Stand
> und die aktuelle Planungsrichtung.

---

## Umgesetzt (Stand Juli 2026)

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
- [x] 5 Unit-Test-Suiten

---

## In Arbeit / Festeigung

- [ ] Doku vollständig auf aktuellen Repo-Stand bringen (laufend)
- [ ] Performance-Verhalten bei sehr großen Sessions beobachten
- [ ] Weitere Tests für Repository-/ViewModel-Logik

---

## Geplant / Richtung (Details folgen)

- **LangChain4j + ToolRegistry** – Agent-Framework mit registrierbaren Tools
- **On-Device-ML** – LiteRT (LLM-Inference) + MediaPipe (Vision/Audio)
- **Vision** – Bilder analysieren (Kamera/Galerie)
- **Bildgenerierung** – remote/on-device, speichern im Projekt-Ordner
- **Bild-Skalierung/-Bearbeitung**
- **Speech-to-Text / Text-to-Speech**
- **Datei-/Bild-Attachments im Chat**
- **Ordnerstruktur erweitern** – Medien, Audio, Exports
- **Modulare Architektur** – feature-/modulbasierte Struktur
- **Session-Löschung** – einzelne Sessions im UI löschen
- **Agent-Templates** – Konfigurationen speichern/wiederverwenden
- **Export** – Projekt-Daten exportieren
- **Error-Boundary** – robustere Fehlerbehandlung in der UI

---

## Aktuelle Non-Goals

- Multi-Agent-Orchestrierung / Debating
- RAG / Vector Store
- Geplante Automatisierung (WorkManager)
- Termux-Hub

Diese Punkte sind nicht grundsätzlich ausgeschlossen, werden aber erst nach
der ToolRegistry- und Modul-Phase priorisiert.
