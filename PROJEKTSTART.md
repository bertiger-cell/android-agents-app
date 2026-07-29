# Projektstart – Workflow & Lebenszyklus

Dieses Dokument beschreibt, wie neue Projekte erstellt werden und wie der
tägliche Arbeitsablauf zwischen Coder und Architekt funktioniert.

**Status:** Konzept / vorbereitet – noch nicht implementiert.

---

## 1. Setup: Projekt erstellen + Initialisierung

### Schritt 1 – User erstellt Projekt

Im bestehenden Create-Project-Dialog (ProjectListScreen):

- **Name**: Kurzer Projektname
- **Beschreibung**: Freitext, 1-3 Absätze. Enthält die Projekt-Idee, gewünschte
  Features, Referenz-Links, Tech-Stack-Wünsche.
- **Erstellen** → `folderPath` wird angelegt, `diary.md` initialisiert

### Schritt 2 – "Projekt-Setup generieren" (NEU)

Nach dem Erstellen erscheint ein Button "Projekt-Setup generieren".
Ein API-Call (gleicher Provider wie der aktive Architect) erzeugt auf Basis
der Projekt-Beschreibung + hinterlegter Templates (AGENTS.md, etc.) alle
Markdown-Dateien auf einmal:

| Datei | Zweck |
|-------|-------|
| `AGENTS.md` | Rollen + Skills, abgestimmt auf das Projekt |
| `ARCHITECTURE.md` | Package-Struktur, Patterns, Provider, Non-Goals |
| `ROADMAP.md` | Meilensteine, Prioritäten, geschätzte Aufwände |
| `TASKS.md` | Task Board (anstehend, in Bearbeitung, erledigt) |
| `RULES.md` | Projektspezifische Arbeitsregeln |
| `SESSION_START.md` | Angepasster Start-Prompt für Agenten |

### Schritt 3 – Review & Anpassung

User sieht die generierten Dateien als Vorschau und kann:
- Einzelne Dateien manuell editieren
- Den gesamten Durchlauf mit geänderter Beschreibung wiederholen
- Direkt in die Arbeit starten

---

## 2. Daily Work: Task Board (Empfehlung 1)

Nach dem Setup liegt `TASKS.md` im Projekt-root. Es ist das Steuerungsinstrument
für den gesamten Entwicklungsprozess.

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
  (geplant)  (Coder)   (Architekt)  (Coder)    (Architekt) (fertig)
```

### Regeln (in RULES.md hinterlegt)

- Jede Session beginnt mit Lesen von `TASKS.md`
- Start eines Tasks → `[ ]` auf `[wip]` setzen + `@Coder`
- Nach Fertigstellung → `[wip]` auf `[review]`
- Architekt reviewed → `[review]` auf `[x]` oder `[changes]`
- Kein Task gilt als erledigt ohne Architekt-Freigabe

---

## 3. Qualität: Review-Zyklus (Empfehlung 2)

Der Architekt prüft nicht alles auf einmal, sondern Task für Task (Regel 12).

### Ablauf pro Task

```
1. Coder schließt Task ab
   → markiert [review] in TASKS.md
   → schreibt Eintrag in diary.md (was, warum, offene Punkte)

2. Architekt prüft
   → liest Diary + Code-Diff
   → schreibt Review-Ergebnis in diary.md
   → setzt [x] oder [changes]

3. Bei [changes]:
   → Coder überarbeitet
   → geht zurück zu Schritt 1

4. Bei [x]: Task gilt als abgeschlossen
```

### Diary-Format

```markdown
## 2024-07-29 05:15 – Coder
SettingsScreen um StatusIndicator erweitert.
Braucht Review: Soll Status gecached werden?

## 2024-07-29 05:30 – Architekt
Approved. Indicator sauber. Kein Cache nötig (API-Call billig).
```

---

## 4. Rollen & Verantwortung

### Coder
- Nimmt Tasks aus "Anstehend"
- Implementiert gemäß ARCHITECTURE.md
- Markiert fertige Tasks zur Review
- Dokumentiert Entscheidungen im Diary

### Architekt
- Prüft Code Task für Task
- Genehmigt oder fordert Änderungen
- Pflegt die Architektur-Dokumentation
- Stellt sicher, dass RULES.md eingehalten wird

### Settings (Architect-Konfiguration)
Der Architect hat eine eigene System-Instruction, die in den App-Settings
hinterlegt und angepasst werden kann:
- System Prompt (bestimmt Verhalten + Prüf-Schwerpunkte)
- (Optional) Model + Temperatur

---

## 5. Abhängigkeiten & Voraussetzungen

Für die Implementierung nötig:

| Komponente | Aufwand | Status |
|------------|---------|--------|
| `ArchitectConfig` in DataStore (System Prompt) | ~10 min | Nicht implementiert |
| `SetupWizardScreen` oder erweiterter Dialog | ~1h | Nicht implementiert |
| Datei-Generierung via API-Call | ~30 min | Nicht implementiert |
| TASKS.md-Logik in SESSION_START.md | ~10 min | Nicht implementiert |
| Review-Zyklus in RULES.md | Bereits dokumentiert (Regel 12+15) | ✅ Fertig |

---

## 6. Nicht-Ziele (für diese Version)

- **Kein Vector Store / RAG.** Diary + Task Board sind Markdown-Text.
- **Kein Multi-User.** Alle Agenten teilen sich denselben Workspace.
- **Keine automatische Task-Zuweisung.** User entscheidet, wer was macht.
- **Kein externes Project-Management.** Kein Jira/Linear-Export.
