# Regeln für opencode – android-agents-app

Diese Datei wird JEDEM Prompt an opencode vorangestellt oder als Kontext
mitgegeben. Ziel: keine unaufgeforderten Änderungen, keine erfundene
Verifikation, ein Task = eine überprüfbare Änderung.

## Harte Regeln

1. **Nur die explizit genannte(n) Datei(en) anfassen.** Keine "während ich
   dabei war"-Refactorings an anderen Dateien. Wenn eine andere Datei
   geändert werden müsste, damit der Task funktioniert: stoppen und fragen,
   nicht selbst entscheiden.

2. **Keine Verifikation behaupten, die nicht stattgefunden hat.** "Datei X
   geprüft" oder "Build erfolgreich" nur schreiben, wenn die Datei tatsächlich
   gelesen bzw. der Build tatsächlich ausgeführt wurde. Im Zweifel: sagen,
   dass es nicht geprüft wurde.

3. **Keine neuen Dependencies ohne Rückfrage.** Jede neue Library in
   `build.gradle.kts` ist eine Entscheidung, keine Nebensache. Vorschlagen,
   nicht einfach hinzufügen.

4. **Bei Ambiguität fragen, nicht raten.** Wenn der Prompt mehrdeutig ist
   (z.B. "füge Fehlerbehandlung hinzu" ohne zu sagen wie), zuerst die
   Interpretation nennen und auf Bestätigung warten, statt einfach loszubauen.

5. **ARCHITECTURE.md ist bindend.** Provider-Interface, Package-Struktur und
   Non-Goals aus `ARCHITECTURE.md` dürfen nicht stillschweigend abgewichen
   werden. Wenn eine Abweichung sinnvoll erscheint: das explizit benennen und
   begründen, nicht einfach anders bauen.

6. **Kein Code für Non-Goals.** Tool-Calling, Multi-Agenten, On-Device-
   Inferenz, RAG, Streaming – siehe `ARCHITECTURE.md` – werden nicht "schon
   mal vorbereitet" oder als Stub eingebaut, solange sie nicht explizit
   angefordert wurden.

7. **Bestehenden Code-Stil respektieren.** Keine Formatierungs- oder
   Namenskonventions-Änderungen an Code, der nicht Teil des aktuellen Tasks
   ist.

## Prompt-Vorlage (pro Task ausfüllen)

```
Aufgabe: <eine konkrete, abgeschlossene Änderung>
Betroffene Datei(en): <exakte Pfade, keine "und ggf. weitere">
Erwartetes Ergebnis: <was soll danach funktionieren/existieren>
Nicht tun: <was explizit außerhalb des Scopes liegt>
```

**Beispiel:**
```
Aufgabe: Erstelle das Interface AiProvider.kt gemäß ARCHITECTURE.md
Betroffene Datei(en): app/src/main/java/.../data/provider/AiProvider.kt
Erwartetes Ergebnis: Interface kompiliert, keine Implementierung enthalten
Nicht tun: Keine der drei Provider-Implementierungen anlegen, keine
Änderungen an build.gradle.kts
```

## Nach jeder Änderung (manuell durch dich, Robert – nicht durch opencode)

- Diff selbst lesen, bevor committed wird
- Prüfen: wurde nur die angeforderte Datei geändert?
- Prüfen: steht eine Behauptung im Output, die nicht stimmen kann (z.B.
  "getestet", obwohl kein Build/Test lief)?
- Erst danach: Commit mit Message, die erklärt was und warum

## Ergänzende Regeln (nach Lernerfahrung mit Mehrfach-Datei-Commits)

8. **Build-Verifikation nach JEDER einzelnen Änderung, nicht gesammelt
   am Ende.** Nach jeder Datei, die geändert wird: `./gradlew
   assembleDebug` (oder `compileDebugKotlin` für schnellere Zwischen-
   checks) tatsächlich ausführen und das Ergebnis zeigen, BEVOR die
   nächste Änderung beginnt. Ein Commit mit der Message "fix: Build-
   Fehler" direkt nach mehreren Feature-Commits ist ein Zeichen dafür,
   dass diese Regel übersprungen wurde – genau das soll sie verhindern.

9. **Harte Datei-Scope-Grenze: max. 2 Dateien pro Aufgabe ohne
   Rückfrage.** Erfordert eine Änderung mehr als 2 Dateien (z.B. ein
   Cleanup, der Gradle, Manifest, Models und Theme gleichzeitig
   betrifft): zuerst die vollständige Dateiliste + kurze Begründung
   pro Datei zeigen, auf Bestätigung warten, danach in der gezeigten
   Reihenfolge EINZELN abarbeiten und nach jeder Datei bauen (siehe
   Regel 8). Nicht "im Rutsch" mehrere unabhängige Cleanups (Gradle-
   Dependency entfernen + Manifest-Permission entfernen + Theme-Fix)
   in einem Commit bündeln, auch wenn sie thematisch alle "Cleanup"
   heißen.

10. **Keine neuen Architektur-Komponenten ohne vorherige
    ARCHITECTURE.md-Ergänzung.** Ein neuer Android-Service, Receiver,
    eine neue Permission, eine neue Library-Dependency oder ein neuer
    Ordner mit eigenständiger Verantwortung (wie z.B. ein
    `automation/`-Paket mit ForegroundService + BootReceiver) wird
    zuerst als Abschnitt in ARCHITECTURE.md skizziert (Zweck, warum
    nötig, welche Permissions das erfordert) – erst nach Freigabe
    wird Code dafür geschrieben. Skeleton-Code "for later" ohne
    Architektur-Eintrag zählt als Verstoß gegen diese Regel, auch wenn
    er (noch) nirgends verdrahtet ist.

## Skills-Ausführungsregeln

11. **LiteRT-Pflicht bei lokalen KI-Modellen.** Bei Nutzung des Skills
    `android-local-ai-integration` ist für On-Device-Inferenz strikt
    **LiteRT** zu verwenden, nicht das ältere TFLite-Format.

12. **Schrittweise Code-Kontrolle durch den Architekt-Agenten.** Der
    Architekt prüft Code-Komponenten immer **nach und nach** (nicht
    alles auf einmal), um die Übersicht zu behalten und gezielt
    Feedback pro Komponente geben zu können.

13. **Material 3 Compliance für `compose-ui-generator`.** Der Skill
    `compose-ui-generator` muss sich an alle Material Design 3
    Design-System-Vorgaben halten (Farbschemata, Typografie, Shapes,
    Surface-Elevationen). Keine Fallbacks auf Material 2.

14. **Structured Concurrency für alle Coroutines.** Sämtliche
    Coroutine-Aufrufe im Projekt müssen sauberes Structured
    Concurrency aufweisen (keine globalen `GlobalScope`-Aufrufe,
    korrektes `viewModelScope`/`lifecycleScope`, Behandlung von
    Cancellation und Exception-Handling in Coroutine-Blöcken).
