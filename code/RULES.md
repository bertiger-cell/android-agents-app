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
