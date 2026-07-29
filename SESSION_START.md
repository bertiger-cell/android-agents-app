# Session-Start-Prompt

Diesen Text am Anfang JEDER Arbeitssitzung mit opencode als ersten
Prompt schicken – auch wenn schon lange dabei, auch wenn "nur kurz
was fixen".

---

Bevor du irgendetwas änderst: Lies AGENTS.md, ARCHITECTURE.md und
RULES.md vollständig aus dem Projekt-Root.

Bestätige, dass du sie gelesen hast, indem du kurz zusammenfasst:
1. Die aktuellen Non-Goals aus ARCHITECTURE.md
2. Die Datei-Scope-Grenze aus RULES.md (wie viele Dateien max. ohne
   Rückfrage)
3. Wann du bauen musst (nach jeder Datei oder erst am Ende?)

Halte dich für die GESAMTE Sitzung an RULES.md, auch wenn ich dir
mehrere Sachen hintereinander gebe, ohne es jedes Mal zu wiederholen:
- Max. 2 Dateien pro Aufgabe ohne Rückfrage (Regel 9)
- Nach JEDER einzelnen Datei-Änderung bauen und Ergebnis zeigen, bevor
  die nächste beginnt (Regel 8)
- Keine neuen Services, Receiver, Permissions, Dependencies oder neuen
  Package-Ordner, ohne das vorher in ARCHITECTURE.md einzutragen und
  auf meine Freigabe zu warten (Regel 10)
- Bei Mehrdeutigkeit fragen, nicht raten (Regel 4)
- Keine Verifikation behaupten, die nicht stattgefunden hat (Regel 2)
- Skills-Ausführungsregeln aus RULES.md (Regeln 11-14) beachten
  (LiteRT-Pflicht, schrittweise Kontrolle, M3-Compliance, Structured
  Concurrency)

Wenn du merkst, dass eine Aufgabe größer wird als ursprünglich gedacht
(mehr Dateien, neue Komponente, unklare Anforderung): stopp, sag mir
warum, warte auf meine Antwort – auch mitten in einer Aufgabe.

## Initialisierung: Rollen & Skills laden

Vor jeder Code-Generierung oder -Analyse sind folgende Kontexte zu laden:

1. **AGENTS.md** – Rollenbeschreibungen (Coder + Architekt) und deren
   zugehörige Skills vollständig einlesen.
2. **RULES.md** – Globale Ausführungsregeln (insbes. Regeln 8-14)
   für die Skills als verbindliche Vorgaben laden.
3. **ARCHITECTURE.md** – Architektur-Entscheidungen und Non-Goals
   prüfen, um Abweichungen zu vermeiden.

Erst wenn alle drei Kontexte geladen sind, darf Code generiert oder
analysiert werden.

### 4. Project Diary lesen (falls vorhanden)
Prüfe, ob im Projekt-Root eine Datei `diary.md` existiert. Falls ja,
lies sie ein – sie enthält Entscheidungen und Ergebnisse vorheriger
Sessions. Nach jeder abgeschlossenen Änderung wird ein neuer Eintrag
angefügt (siehe RULES.md Regel 15).
