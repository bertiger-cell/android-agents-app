# Architect Agent Skill

## Purpose

Ermöglicht neu erstellten Projekten eine automatisierte **Discovery-Phase**:
Ein spezialisierter AI-Agent (der "Architect") interviewt den User, analysiert
die Projekt-Idee und generiert daraus ein vollständiges Projekt-Scaffold
(ARCHITECTURE.md, ROADMAP.md, RULES.md, AGENTS.md) plus passende KI-Agenten.

## Flow Overview

```
User erstellt Projekt
  → CreateProjectDialog
    → onCreateProjectWithArchitect(name, description)
      → AgentViewModel.createProject()           [DB: ProjectEntity]
      → AgentViewModel.createArchitectDiscoverySession()
        → createOrGetArchitectAgent()            [DB: Agent "Architect"]
        → getOrCreateSession(..., "Project Discovery")
        → addMessage(introMessage)               [DB: Message]
      → ChatScreen öffnet mit Architekt-Chat
        → User + Architect chatten (3 Phasen)
          → Phase 1: Warm-Up
          → Phase 2: Strukturiertes Interview
          → Phase 3: JSON-Generierung
        → extractAndParseArchitectJSON()          [Parser]
          → saveProjectScaffold()                 [JSON → ProjectScaffold]
          → discovery.json                        [File]
        → showArchitectSummary = true
      → ArchitectSummaryScreen öffnet (Dialog)
        → User reviewed Plan, editiert Prompts
        → "Fertig!" → createAgentsFromScaffold()
          → ProjectFileWriter schreibt .md-Dateien  [File: ARCHITECTURE.md, etc.]
          → Agenten aus suggestedAgents anlegen     [DB: Agent]
          → diary.md wird aktualisiert              [File]
          → Toast: "Projekt-Dateien erstellt..."
        → Summary schließt → zurück zu ProjectDetail
```

## 3 Phasen des Architect System Prompts

Der Prompt in `DEFAULT_ARCHITECT_PROMPT` (AgentModels.kt) definiert:

| Phase | Nachrichten | Ziel |
|---|---|---|
| **WARM-UP** | 5-10 | Offene Fragen, Brainstorming, Links sammeln |
| **INTERVIEW** | 10-15 | Gezielte Domain-Fragen (Skalierung, Latenz, Auth, etc.) |
| **GENERIERUNG** | letzte | JSON mit discovery_context, phases, rules, suggested_agents |

Der Prompt ist **editierbar** in den Settings (DataStore-backed).

## File Structure

```
app/src/main/java/com/agents/app/
├── data/
│   ├── ArchitectConfigRepository.kt     # DataStore: Custom Prompt speichern/laden
│   └── ProjectFileWriter.kt             # Schreibt .md-Dateien in project folder
├── models/
│   └── AgentModels.kt                   # ProjectDiscovery, ProjectScaffold,
│                                        # ScaffoldPhase, AgentSpec,
│                                        # DEFAULT_ARCHITECT_PROMPT
├── ui/
│   ├── AgentViewModel.kt               # Architect-Logik (createOrGetArchitectAgent,
│                                        # createArchitectDiscoverySession,
│                                        # extractAndParseArchitectJSON,
│                                        # createAgentsFromScaffold)
│   ├── screens/
│   │   ├── ArchitectSummaryScreen.kt    # Plan-Review + Prompt-Editor
│   │   └── SettingsScreen.kt            # Architect-Prompt-Editor in Settings
│   └── navigation/
│       └── AppNavigation.kt             # Discovery-Session bei createProject,
│                                        # Summary-Modal, Loading-Overlay, Toast
├── AgentRepository.kt                   # saveProjectScaffold() JSON-Parser
```

## Key Data Classes (AgentModels.kt)

```kotlin
data class ProjectDiscovery(
    projectId, domain, technologies, experienceLevel,
    projectSize, concerns, links, keyInsights
)

data class ProjectScaffold(
    discoveryContext: ProjectDiscovery,
    architecture: String,
    phases: List<ScaffoldPhase>,
    rules: List<String>,
    suggestedAgents: List<AgentSpec>,
    diaryEntry: String
)

data class ScaffoldPhase(phaseNumber, name, description, duration, focus)

data class AgentSpec(name, description, systemPrompt, provider, model, temperature)
```

## JSON Extraction (saveProjectScaffold)

- **Robust substring extraction:** `indexOf('{')` + `lastIndexOf('}')`
  statt Regex (vermeidet Bug bei verschachtelten Objekten)
- `JSONObject` / `JSONArray` aus Android SDK (keine Extra-Dependency)
- Optional-Felder via `optString` / `optJSONArray` (keine NPE bei fehlenden Feldern)
- Fehler: Log + `null` zurück (kein Crash, Chat kann fortgesetzt werden)
- JSON wird als `discovery.json` im Project-Ordner gespeichert

## Configuration

| Setting | Ort | Default |
|---|---|---|
| Architect System Prompt | Settings → Architect | `DEFAULT_ARCHITECT_PROMPT` |
| Auto-Summary Toggle | ViewModel | `true` |
| Architect Model | DataStore-Konfigurationswert | Default: `gpt-4o` |
| Provider | DataStore-Konfigurationswert | Default: `openrouter` |

## Extending

### Neuen Suggested-Agent-Typ hinzufügen:
1. `AgentSpec`-Instanz im JSON (wird vom Architect generiert)
2. Optional: Neuen Prompt in `DEFAULT_ARCHITECT_PROMPT` erwähnen

### Prompt verbessern:
1. Settings → Architect → System-Prompt editieren
2. Oder `DEFAULT_ARCHITECT_PROMPT` in `AgentModels.kt` ändern

### Neues File generieren:
1. Methode in `ProjectFileWriter.kt` hinzufügen
2. In `writeProjectFiles()` aufrufen

## Edge Cases

| Case | Handling |
|---|---|
| JSON nicht gefunden | Log.w + `null` zurück, kein Crash |
| JSON invalid | JSONException → Log.e + `null`, Chat fortsetzbar |
| Kein API-Key | ChatScreen zeigt Fehlermeldung |
| Discovery-Session läuft | `getOrCreateSession()` gibt existierende zurück |
| Project-Ordner fehlt | `getProjectPath()` fallback zu `project_$id` |
| createAgentsFromScaffold Fehler | try/catch → Toast-Fehlermeldung |
| Loading während Generation | Overlay + disabled fertig-Button |

## Build & Test

- Keine neuen Dependencies (nur Android SDK Bordmittel)
- Keine Room-Migration (Schema hat sich nicht geändert)
- Test: Projekt erstellen → Discovery startet → Chat → JSON → Summary → Files
