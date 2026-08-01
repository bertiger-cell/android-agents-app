# DESIGN.md – Agent Studio UI

## Design Direction

Moderner Dark-Tech-Look mit hohem Kontrast und klarer Struktur. Deep Background,
violette Akzente und Cyan als Sekundärfarbe. Material 3 als Basis, wenig visueller
Clutter. Jedes Element soll funktional begründet sein.

Die App nutzt auf Android 12+ standardmäßig Dynamic Color. Die hier beschriebene
Palette ist der Dark-Fallback und die Referenz für den gewünschten Look.

## Color System

### Dark Fallback

- Background: `#0D0D0D`
- Surface: `#1A1A1A`
- Surface Variant: `#252525`
- Primary: `#BB86FC`
- Primary Container: `#3700B3`
- Secondary: `#03DAC6`
- Secondary Container: `#018786`
- Tertiary: `#FF6B6B`
- Outline: `#938F99`
- On-Background: `#E6E1E5`
- On-Primary: `#000000`
- On-Surface: `#E6E1E5`
- On-Surface Variant: `#CAC4D0`
- Error: `#CF6679`

### Contrast Rules

- Kein reines Weiß auf reinem Schwarz für Fließtext
- Body-Text mindestens 4.5:1 Kontrast
- Primäre Buttons: violett (`#BB86FC`) mit schwarzem Text
- Danger-Aktionen: Tertiary/Error-Farbe mit hellem Text
- Disabled: reduzierte Opacity, keine Interaktion

## Typography

Material 3 Default-Typografie mit System-Font (Roboto auf Android).

- Display Large: 57sp / 64sp (wird aktuell nicht als Splash genutzt)
- Headline Small/Medium: 24sp / 28sp (Seitentitel, Empty States)
- Title Medium: 16sp / 24sp / Medium (Sektionen, Projektnamen)
- Body Large: 16sp / 24sp (Chat-Inhalte)
- Body Small: 12sp / 16sp (Metadaten)
- Label Large: 14sp / 20sp / Medium (Buttons)

## Spacing & Layout

- Basis-Raster: 4dp
- Screen-Padding: 16dp horizontal
- Section-Spacing: 24dp
- Element-Spacing: 8dp-16dp
- Karten-Padding: 12dp-16dp
- Shape-System: 8dp / 12dp / 16dp / 24dp / 32dp

## Component Patterns

### Buttons

- Primary (Filled): `MaterialTheme.colorScheme.primary`, schwarzer Text im Dark-Fallback
- Secondary (Outlined): Outline-Border, on-surface Text
- Danger: `MaterialTheme.colorScheme.error`, heller Text
- Disabled: Material 3 Default, keine Interaktion

### Cards

- RoundedCornerShape 8dp-16dp je Kontext
- `surfaceVariant` für Listen-Karten
- `primaryContainer` / `secondaryContainer` für Hervorhebungen
- Keine Schatten, stattdessen Color-Depth

### Input Fields

- `OutlinedTextField` mit `focusedBorderColor = primary`
- API-Keys mit Show/Hide-Toggle
- Modelle und Provider als `ExposedDropdownMenu`

### TopAppBar

- `containerColor = primary`
- Titel + Back/Settings-Action
- Keine Elevation

### FAB

- Dashboard: Plus für neues Projekt
- Primary Container oder Primary Default

### Chat Bubbles

- User: `primary` Farbe, schwarzer Text, rechtsbündig
- Assistant: `secondaryContainer` Farbe, on-secondary-container Text, linksbündig
- Max Width: 300dp
- Radius: 16dp, kleine Ecke zur jeweiligen Sprechrichtung
- Token-Anzahl als `labelSmall` unter dem Inhalt

## Animation & Motion

- Navigation: 400ms Fade + Slide
- Loading-Overlay: zentrierter `CircularProgressIndicator`
- Chat-Auto-Scroll: animiert bei neuen Nachrichten, wenn User nahe am Ende ist
- Kein Intro-Walk mehr; die App startet direkt im Dashboard

## Screen Layouts

### Dashboard / ProjectListScreen

- TopAppBar: „Agent Studio“ + Projektanzahl + Settings-Icon
- Leerer Zustand: Welcome-Hero-Card mit „Neues Projekt mit Architect“
- Gefüllter Zustand: „Weiter geht's!“-Card, Projekt-Cards, FAB
- Projekt-Cards: Name, Beschreibung, Datum, Löschen-Icon
- Create-Dialog: Name + Beschreibung, startet danach automatisch den Architect
- Delete-Dialog: Bestätigung für Projekt inklusive Hinweis auf zugehörige Daten

### Project Detail / ProjectDetailWithTabsScreen

- TopAppBar: Projektname, Agentenzahl, Back, Edit-Icon
- Tabs: `Agenten`, `Sessions`, `Dateien`
- Agenten-Tab: Empty State mit „Agent erstellen“ + „Discovery wiederholen“
- Agenten-Karte: Name, Provider/Modell, Sessions, „Neuer Chat“, Delete
- Sessions-Tab: Liste aller Sessions des Projekts
- Dateien-Tab: File-Viewer mit Größe, Änderungsdatum, Refresh und Inhalt-Dialog
- Edit-Dialog: Projektname und Beschreibung
- Delete-Agent-Dialog: Bestätigung inklusive Cascade-Hinweis

### Create Agent / CreateAgentScreen

- TopAppBar mit Back
- Quick-Start-Templates
- Provider-Dropdown
- Modelle laden je Provider (Ollama, OpenRouter, Zen)
- System-Prompt als mehrzeiliges Textfeld
- Temperatur-Regler
- Save/Create Action

### Settings / SettingsScreen

- TopAppBar mit Back
- Provider-Sektionen: OpenRouter, OpenCode Zen, Ollama
- API-Keys mit Show/Hide
- Ollama Base URL + Test-Connection
- Architect-Settings als aufklappbare Card
- Architect: Provider, Modell, System-Prompt, Speichern/Abbrechen

### Chat / ChatScreen (Fullscreen-Modal)

- TopAppBar: Session-Titel (klickbar für Rename), Agentenname, Back
- Nachrichtenliste mit Auto-Scroll
- Typing-Indicator während Streaming
- Input-Zeile: `OutlinedTextField` + Send-Icon
- Rename-Dialog: Chat-Titel ändern

### Architect Summary / ArchitectSummaryScreen (Fullscreen-Dialog)

- TopAppBar: „Projekt-Plan“ + Close
- Projekt-Kontext-Card: Domain, Technologien, Level, Größe, Concerns
- Roadmap-Sektion mit Phasen-Karten
- Regeln-Sektion
- Agenten-Sektion mit Edit-Button pro Agent
- Edit-Dialog für System-Prompts
- Aktionen: „Abbrechen“ und „Fertig!“

## Navigation & Overlays

Routen in `AppNavigation.kt`:

- `projects` – Dashboard/Projektliste
- `project/{projectId}` – Detail mit 3 Tabs
- `create-agent/{projectId}` – Agent anlegen
- `settings` – Einstellungen

Overlays über der Navigation:

- Chat-Modal bei ausgewählter Session
- Architect-Summary bei `showArchitectSummary = true`
- Loading-Overlay während Datei-Generierung
- Toast bei Erfolg/Fehler der Generierung

## Status

Diese Datei beschreibt den aktuellen Implementierungsstand der UI. Nicht mehr
vorhandene Screens (`IntroScreen`, `HomeScreen`, `AgentListScreen`) sind entfernt.
