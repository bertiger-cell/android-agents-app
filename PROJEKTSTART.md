# Projektstart – Workflow & Lebenszyklus

Dieses Dokument beschreibt, wie neue Projekte erstellt werden und wie der
tägliche Arbeitsablauf zwischen Coder und Architekt funktionieren soll.

**Status:** Grundlegender Flow ist implementiert. Der geplante
Task-Board- und Review-Workflow (TASKS.md) ist noch nicht automatisiert.

---

## 1. Setup: Projekt erstellen + Initialisierung

### Schritt 1 – User erstellt Projekt

Im Create-Project-Dialog (Dashboard):

- **Name**: Kurzer Projektname
- **Beschreibung**: Freitext, 1-3 Absätze. Enthält die Projekt-Idee, gewünschte
  Features, Referenz-Links, Tech-Stack-Wünsche.
- **Erstellen** → `folderPath` wird angelegt, `diary.md` initialisiert

### Schritt 2 – Architect-Discovery (implementiert)

Nach dem Erstellen startet automatisch das **Architect-Interview**:

1. **Warm-Up** – offene Fragen, Brainstorming
2. **Strukturiertes Interview** – domain-tailored Fragen
3. **Generierung** – JSON-Projektplan

Der Architect generiert daraus:

| Datei | Zweck |
|-------|-------|
| `AGENTS.md` | Rollen + Skills, abgestimmt auf das Projekt |
| `ARCHITECTURE.md` | Package-Struktur, Patterns, Provider, Non-Goals |
| `ROADMAP.md` | Meilensteine, Prioritäten, geschätzte Aufwände |
| `RULES.md` | Projektspezifische Arbeitsregeln |
| `SKILLS.md` | Skills für die Projektarbeit |
| `diary.md` | Projekt-Tagebuch |
| `discovery.json` | Roh-JSON des Plans |

### Schritt 3 – Review & Anpassung (implementiert)

Der User sieht den Plan im **Architect Summary Screen** und kann:
- Einzelne Agent-Prompts editieren
- Den Plan bestätigen ("Fertig!") oder abbrechen
- Die Discovery bei Bedarf neu starten

---

## 2. Daily Work: Task Board (geplant, nicht implementiert)

Der geplante Workflow sieht ein `TASKS.md` im Projekt-Root vor:

### Task-Formate

```markdown
## Anstehend
- [ ] Settings: Verbindungsstatus anzeigen (#12)

## In Bearbeitung
- [wip] Chat-Titel generieren (#11) – @Coder

## Review
- [review] Ollama-Statusanzeige (#12) – @Coder → @Architekt

## Erledigt
- [x] Room-Indices hinzugefügt (#10)
```

### Status-Übergänge

```
[ ]  →  [wip]  →  [review]  →  [changes]  →  [review]  →  [x]
```

---

## 3. Qualität: Review-Zyklus (Regeln dokumentiert, Automatisierung offen)

Der Architekt prüft Code-Komponenten nach und nach (Regel 12).

### Ablauf pro Task

```
1. Coder schließt Task ab
   → markiert [review] in TASKS.md (sobald TASKS.md existiert)
   → schreibt Eintrag in diary.md

2. Architekt prüft
   → liest Diary + Code-Diff
   → schreibt Review-Ergebnis in diary.md
   → setzt [x] oder [changes]

3. Bei [changes]: Coder überarbeitet

4. Bei [x]: Task gilt als abgeschlossen
```

---

## 4. Rollen & Verantwortung

### Coder
- Nimmt Tasks aus "Anstehend"
- Implementiert gemäß ARCHITECTURE.md
- Dokumentiert Entscheidungen im Diary

### Architekt
- Führt das Discovery-Interview durch
- Generiert Projektplan + Dateien
- Prüft Code Task für Task
- Pflegt die Architektur-Dokumentation

### Settings (Architect-Konfiguration)
Der Architect hat eigene System-Instructions, die in den App-Settings
hinterlegt und angepasst werden können:
- System Prompt (bestimmt Verhalten)
- Provider (OpenRouter, Zen, Ollama)
- Modell

---

## 5. Implementierungsstatus

| Komponente | Aufwand | Status |
|------------|---------|--------|
| `ArchitectConfig` in DataStore (System Prompt) | ~10 min | ✅ Implementiert |
| Architect-Interview + Discovery | — | ✅ Implementiert |
| Datei-Generierung (Architect/ProjectFileWriter) | — | ✅ Implementiert |
| Summary-Screen mit Prompt-Editing | — | ✅ Implementiert |
| TASKS.md-Logik | — | ⬜ Geplant |
| Review-Zyklus | — | 📄 Dokumentiert (Regel 12+15) |

---

## 6. Nicht-Ziele (für diese Version)

- **Kein Vector Store / RAG.** Diary + Task Board sind Markdown-Text.
- **Kein Multi-User.** Alle Agenten teilen sich denselben Workspace.
- **Keine automatische Task-Zuweisung.** User entscheidet, wer was macht.
- **Kein externes Project-Management.** Kein Jira/Linear-Export.
