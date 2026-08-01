# Changelog

## v2.0 (Phase 2, umgesetzt und committet)

- Projekt-Export/Import als ZIP (Phase 2a): `ProjectTransferRepository`,
  `manifest.json` (Gson), SAF-UI in der Projekt-Detail-TopAppBar
- Session-Löschung einzeln im UI mit Bestätigungsdialog
- Agent-Templates: persistente Vorlagen via DataStore (`agent_templates`),
  speichern/laden/löschen im Create-Agent-Screen
- Projekt-Ordnerstruktur: `media/`, `audio/`, `exports/` bei Projekt-Erstellung
- Chat-Attachments: Room v3 + `message_attachments`, Migration 2->3,
  Bild-/Datei-Picker, Vorschau und Chips im Chat, Export im Manifest
- Error-Boundary: zentraler `uiError`-State + Snackbar-Feedback
- Feature-Pakete statt `ui/screens` (`feature/projects`, `chat`, `agents`, `settings`)
- Neue Tests: Transfer-Roundtrip, Templates, Ordnerstruktur, Attachment-Manifest
  (insgesamt 10 Suiten / 53 Tests)

## v1.0

- Optionaler Architect-Agent mit Settings-Toggle
- Persistente Architect-Konfiguration via DataStore
- Robuste Navigation mit Projects, Chat und Settings
- Chat-UI mit klaren Message-Bubbles, Loading-State und Retry-Error-State
- Streaming-Tests fuer OpenAI-kompatible Provider und Ollama
- Aktualisierte Dokumentation fuer den aktuellen Repo-Stand
