# Architect Flow – Portable Specification

Ein geführter Discovery-Prozess für neue Projekte. Ein spezialisierter
AI-Agent (der "Architect") interviewt den User und generiert daraus
ein vollständiges Projekt-Scaffold.

## Konzept

Statt leeren Projekt-Templates startet jedes Projekt mit einem
**intelligenten Interview**. Der Architect fragt nicht nach Formulardaten,
sondern führt ein echtes Gespräch:

1. **Warm-Up** – Offene Fragen, Brainstorming, User erzählt seine Idee
2. **Interview** – Gezielte, domain-tailored Fragen
3. **Generierung** – Output als strukturiertes JSON

Das Ergebnis ist ein massgeschneidertes Setup, KEIN generalisiertes Template.

## Der System Prompt

Kern des Architect ist sein System Prompt. Er definiert die 3 Phasen,
den Ton und das JSON-Output-Format.

### Prompt-Prinzipien

```
Phase 1 – WARM-UP (erste 5-10 Nachrichten)
- Stelle OFFENE Fragen ("Erzaehl mir von deinem Traum-Projekt")
- Keine engen Tech-Fragen ("Welche Datenbank?") in Phase 1
- Biete Links an, wenn User Technologien/Patterns erwaehnt
- Sammle intern: Domain, Technologien, Experience Level,
  Projektgroesse, Concerns

Phase 2 – STRUKTURIERTES INTERVIEW (10-15 Nachrichten)
- Nutze Wissen aus Phase 1 fuer gezielte Fragen
- Frage domaenenspezifisch (bei IoT: "Wie viele Geraete parallel?")
- Typische Dimensionen: Skalierung, Latenz, Persistenz, Auth,
  Integration, Sicherheit, Deployment

Phase 3 – GENERIERUNG
- Warte auf "Mach mir einen Plan!" oder ~15 Nachrichten
- Generiere NUR am Ende JSON (nicht zwischendrin)
- JSON muss valide sein
```

### Output-JSON-Struktur

```json
{
  "discovery_context": {
    "domain": "IoT Smart-Home",
    "technologies": ["Kotlin", "Spring Boot", "MQTT"],
    "experience_level": "mid_kotlin_junior_iot",
    "project_size": "medium_startup",
    "concerns": ["Skalierung", "Neue Domain"]
  },
  "architecture": "Kurze Beschreibung der System-Architektur",
  "phases": [
    {
      "phase_number": 1,
      "name": "Learning & Setup",
      "description": "Warum diese Phase wichtig ist",
      "duration": "1 week",
      "focus": "Haupt-Aufgabe der Phase"
    }
  ],
  "rules": [
    "Regel 1: Beschreibung",
    "Regel 2: Beschreibung"
  ],
  "suggested_agents": [
    {
      "name": "Agent-Name",
      "description": "Spezialgebiet",
      "system_prompt": "Ausfuehrlicher System Prompt",
      "provider": "openrouter oder andere",
      "model": "gpt-4o oder andere",
      "temperature": 0.7
    }
  ],
  "diary_entry": "Kurze Zusammenfassung fuer das Projekttagebuch"
}
```

## Implementierungs-Pattern

```
User erstellt Projekt
  → onCreateProject(name, description)
    → Repository.createProject() → DB + folderPath
    → createArchitectAgent(projectId)
      → Check: existiert bereits?
      → Load: System Prompt aus Config/DataStore
      → Create: Agent-Eintrag in Datenbank
    → createDiscoverySession(agentId, "Discovery")
      → Session-Eintrag in Datenbank
      → Intro-Message (Welcome-Text)
  → ChatScreen oeffnet mit Architect
    → User + Architect chatten (3 Phasen)
    → JSON wird als letzte Nachricht generiert
      → extractJSON(message)
        → indexOf('{') + lastIndexOf('}')
        → substring → JSONObject parse
      → saveScaffold(projectId, json)
        → ProjectScaffold-Objekt bauen
        → discovery.json in project folder schreiben
  → SummaryScreen zeigt Plan
    → User reviewed + editiert Prompts
    → "Fertig!" → createAgentsFromScaffold()
      → Dateien schreiben:
        → ARCHITECTURE.md (aus architecture)
        → ROADMAP.md (aus phases)
        → RULES.md (aus rules)
        → SKILLS.md (Standard-Skills)
        → AGENTS.md (aus suggested_agents)
        → diary.md (Append Entry)
      → Agenten aus suggested_agents anlegen
      → Toast: "Fertig!"
```

## JSON Extraktion (robust)

KEIN Regex. Regex bricht bei verschachtelten Objekten.

**Stattdessen:**
```
start = text.indexOf('{')
end   = text.lastIndexOf('}')
if (start >= 0 && end > start) {
    jsonString = text.substring(start..end)
}
```

**Validierung:** `JSONObject(jsonString)` – Exception bei invalidem
JSON → catch + Log + null (kein Crash).

## Konfiguration

| Parameter | Zweck | Empfehlung |
|---|---|---|
| System Prompt | Verhalten des Architect | Siehe Prompt-Prinzipien |
| Modell | Welches LLM | Konfigurierbar (kein Hardcode) |
| Provider | OpenRouter, OpenCode Zen, Ollama | Konfigurierbar |
| Temperature | Kreativitaet | 0.7 |
| Auto-Summary | Summary automatisch anzeigen | true |

## Edge Cases

| Situation | Handling |
|---|---|
| JSON nicht gefunden | Log-Warning, Chat fortsetzbar |
| JSON invalid | Exception catch → Log → null (kein Crash) |
| Kein API-Key | Fehlermeldung im Chat |
| Session unterbrochen | Naechster Chat setzt letzte Session fort |
| suggested_agents leer | Nur Dateien schreiben, keine Agenten |

---

*Portable Specification – Stack-unabhaengig einsetzbar.*
