# Roadmap – android-agents-app → KI-Projekt-Workbench

Alle Ideen aus dem Gespräch, sortiert nach **Phase** (wann), **Aufwand** (wie viel Arbeit) und **Abhängigkeiten** (was muss vorher fertig sein).

---

## V1 – Aktuell (stabil)
✅ Chat mit 3 Providern (OpenRouter, OpenCode Zen, Ollama)
✅ Model Picker (API-gestützt, nicht hardcodiert)
✅ Credentials pro Provider (DataStore persistent)
✅ Message History (komplett, mit Collector-Fix)
✅ Material 3 UI + Dynamic Color
✅ README + ARCHITECTURE.md + RULES.md

---

## V2 – Nächstes (Streaming + Vorlagen) – JETZT STARTEN

### 2a. Streaming-Antworten [Aufwand: MITTEL]
**Was:** Antworten erscheinen wortweise/Token-weise, wie ChatGPT. Nicht "warten bis fertig".
**Warum:** UX fühlt sich responsiv an, Nutzer sieht sofort "es tut sich was".
**Abhängigkeiten:** Keine (on top of V1)
**Betroffene Dateien:**
  - AIProviderService.kt: callOpenAiCompatible(), callOllama() auf Stream-Parsing umstellen
  - AgentViewModel.kt: sendMessage() incremental updaten statt "am Ende speichern"
  - ChatScreen.kt: Text wortweise rendern während ankommt
  - AgentRepository.kt: Message speichern erst am Ende (aktuell: speichert beim Senden)
**Priorität:** HOCH (größter UX-Gewinn)

### 2b. Agent-Vorlagen/Templates [Aufwand: KLEIN]
**Was:** Vordefinierte Agent-Setups zum Schnellstart. z.B. "Code Assistant", "Creative Writer", "Research Mode".
**Warum:** Schneller anfangen, weniger Felder füllen, "Best Practices" entdecken.
**Abhängigkeiten:** Keine (on top of V1)
**Betroffene Dateien:**
  - AgentModels.kt: neue data class AgentTemplate
  - CreateAgentScreen.kt: Template-Dropdown am Anfang hinzufügen
  - AgentViewModel.kt: loadTemplates() + applyTemplate() Funktion
**Priorität:** MITTEL-HOCH (UX-Polish)

### 2c. Dashboard/Home-Screen [Aufwand: KLEIN]
**Was:** Startbildschirm mit Uhrzeit, Datum, Wetter, letzte Aktivität.
**Warum:** Besseres Onboarding, Überblick beim Öffnen.
**Abhängigkeiten:** Keine (on top of V1)
**Betroffene Dateien:**
  - Neue Datei: HomeScreen.kt
  - AppNavigation.kt: HomeScreen als startDestination setzen
  - build.gradle.kts: Weather-API Library (wenn echte Daten, sonst Mock)
**Priorität:** NIEDRIG (nice-to-have)

---

## V3 – Projekt-Layer (Fundament für alles ab hier) [Aufwand: GROSS]
**KRITISCH:** Das muss vor Vision, Image Gen, Automation etc. kommen. Alles braucht einen Projekt-Container zum Speichern von Dateien/Bildern/Outputs.

### 3a. Projects/Folders-Struktur [Aufwand: GROSS]
**Was:** Hierarchie-Rewrite: Projekt → Agenten, Projekt → Chat-Sessions, Projekt → gespeicherte Dateien.
**Warum:** Speicherort für alles (Bilder, Generationen, Exports), logische Gruppierung.
**Abhängigkeiten:** Umfangreicher ARCHITECTURE.md-Umbau ZUERST
**Betroffene Dateien:**
  - AgentModels.kt: +Project, +ChatSession (aktuell: nur Agent/Message)
  - AgentDatabase.kt + DAOs: neue Tables für Projects, Sessions, Files
  - AgentRepository.kt: komplett neu strukturiert
  - Komplette Navigation umbauen (ProjectList → AgentList → ChatScreen)
**Priorität:** KRITISCH (MUSS VOR allem anderen kommen, das Dateien speichert)

### 3b. Projekt-Scaffolder / Planning-Tool [Aufwand: GROSS]
**Was:** Agent stellt Fragen ("Was willst du bauen? Code/Analyse/Content?"), generiert daraus eine Roadmap mit Phasen, benötigten Skills, Checkpoints.
**Warum:** Strukturierter Start statt "ChatGPT, schreib mir was" (genau der Wiki-Fehler).
**Abhängigkeiten:** Projects/Folders (V3a)
**Betroffene Dateien:**
  - Neue Agent-Vorlage im System: "Project Planner Agent"
  - Neue Screen: ProjectPlannerScreen.kt (Wizard-UI für Fragen)
  - MarkdownGenerator.kt: generiert README + Roadmap + Checkpoints
  - Speichert Ergebnis als Projekt-Template + README im Projekt-Ordner
**Priorität:** HOCH (wichtiger als Vision)

---

## V4 – Multimodal (Vision + Image Gen) [Aufwand: GROSS, aber aufgeteilt]

### 4a. Vision / Image Recognition [Aufwand: MITTEL]
**Was:** Agent analysiert Fotos/Screenshots (via Kamera oder Galerie).
**Warum:** "Zeige mir dieses Bild, was siehst du?"
**Abhängigkeiten:** V3a (Projects/Folders zum Speichern von Input-Bildern)
**Betroffene Dateien:**
  - AIProviderService.kt: Neue Methode callWithImage() (OpenRouter/Zen unterstützen multimodal)
  - ChatScreen.kt: Image-Picker-Button, Image-Preview
  - Message-Modell: +imageUri (optional)
  - AgentRepository.chat(): Image-Datei vor Chat speichern
**Priorität:** MITTEL-HOCH (natürliche Feature)

### 4b. Image Generation (DALL-E / Stable Diffusion) [Aufwand: MITTEL]
**Was:** "Generiere ein Bild von..." → Agent ruft Generierungs-API auf, speichert Ergebnis.
**Warum:** Agent kann kreativ werden, nicht nur analysieren.
**Abhängigkeiten:** V3a (Speicherort für generierte Bilder)
**Betroffene Dateien:**
  - AIProviderService.kt: neue Methode generateImage() (für OpenRouter DALL-E, Stable Diffusion, etc.)
  - Message-Modell: +generatedImageUri (optional)
  - ChatScreen.kt: zeige generierte Bilder anders (Gallery statt Chat-Bubble)
**Priorität:** MITTEL (später als Vision, cooler aber weniger praktisch)

### 4c. Image Scaling / Bearbeitung [Aufwand: KLEIN]
**Was:** Skaliere, drehe, zuschneiden generierte/hochgeladene Bilder.
**Warum:** QoL für Bilder.
**Abhängigkeiten:** V4a oder V4b
**Betroffene Dateien:**
  - Neue Util-Klasse: ImageEditor.kt
  - Image-Preview-Screen: mit einfachen Editier-Tools
**Priorität:** NIEDRIG (später, wenn 4a/4b funktioniert)

---

## V5 – Multi-Agent & Automation [Aufwand: GROSS, unabhängige Tracks]

### 5a. Multi-Chat pro Agent [Aufwand: KLEIN]
**Was:** Ein Agent kann mehrere separate Chat-Threads haben (nicht eine Konversation).
**Warum:** Mehrere experimentelle Chats nebeneinander ausprobieren.
**Abhängigkeiten:** V3a (ChatSession-Konzept)
**Betroffene Dateien:**
  - ChatSession-UI erweitern: Liste aller Threads, neuen hinzufügen
**Priorität:** NIEDRIG-MITTEL

### 5b. Multi-Agent Debattieren / Orchestrierung [Aufwand: SEHR GROSS]
**Was:** 2+ Agenten chatten gemeinsam, debattieren über ein Thema.
**Warum:** "Philosophieren zu zweit", echte Agent-Orchestrierung.
**Abhängigkeiten:** V3a (Projects/Sessions)
**Betroffene Dateien:**
  - Message-Modell: +sender (nicht nur "user" vs "assistant", sondern Agent-ID)
  - Komplett neue Orchestrierungs-Logik (wer ist dran, was macht der Nutzer als Moderator)
  - Neue Screen: DebateScreen.kt
**Priorität:** NIEDRIG (später, große Änderung)

### 5c. Termux-Hub (Automation-Button) [Aufwand: MITTEL]
**Was:** Button in der App öffnet Termux mit vorgefertigtem Befehl (opencode, claudecode, llama.cpp).
**Warum:** Externe Tools aus der App starten, mit Agenten arbeiten.
**Abhängigkeiten:** V3a (optional: Agent-Output an Termux übergeben, erfordert IPC)
**Betroffene Dateien:**
  - MainActivity.kt oder neue Util: Intent an Termux schicken
  - SettingsScreen.kt: Termux-Integrationsoptionen (welcher Command, welcher Agent)
  - neue Datei: TermuxHelper.kt
**Priorität:** MITTEL (nice-to-have, aber unabhängig)

### 5d. Geplante Läufe / Automatisierung [Aufwand: GROSS]
**Was:** Agent läuft nach Plan (täglich 10 Uhr, bei Event X) ohne Nutzer-Trigger.
**Warum:** Automation, Batch-Jobs, Agenten sind echte Workers.
**Abhängigkeiten:** V3a + Android WorkManager
**Betroffene Dateien:**
  - Neue Datei: AgentWorker.kt (WorkManager Task)
  - Neue Datei: AutomationRepository.kt
  - SettingsScreen.kt: Automations-Konfiguration
  - Manifest: Permissions für SCHEDULE_EXACT_ALARM
**Priorität:** NIEDRIG (complex, später)

---

## Zusammenfassung: Startplan

**JETZT (nächste 1–2 Wochen):**
1. Streaming (V2a) – größter UX-Gewinn
2. Agent-Vorlagen (V2b) – schnell

**Danach (2–3 Wochen):**
3. Projects/Folders (V3a) – FUNDAMENT, alles blockiert ohne das
4. Project Scaffolder (V3b) – auf V3a aufbauend

**Später (nach V3 komplett):**
5. Vision (V4a) + Image Gen (V4b)
6. Automation/Termux (V5c)
7. Geplante Läufe (V5d)
8. Multi-Agent Debatte (V5b) – am Ende, große Änderung

**Skip (vorerst):**
- Image Scaling (V4c) – später, wenn nötig
- Multi-Chat pro Agent (V5a) – später

---

## Aktuelle Non-Goals (nach wie vor)
- On-Device-Inferenz (das macht bereits AI Workbench + Private Agent)
- Echte RAG (Complex, später)
- Streaming Voice Output (später, falls nötig)
