# Ladder

A terminal platform game written in Java with [TamboUI](https://tamboui.dev).
Climb out of the mine before your air runs out, dodging runaway ore carts and grabbing gems on the way.

## Build and run

Needs Java 25 and Maven.

```sh
mvn package
java --enable-native-access=ALL-UNNAMED -jar target/ladder-0.1.0-SNAPSHOT.jar
```

There are two looks. Switch between them on the title screen with ← → (or Tab), or pick the starting one with `--style`:

| Option | Look |
| --- | --- |
| `--style=ascii` (default) | Plain 7-bit ASCII, green on black like a classic green screen. Works in any terminal. |
| `--style=modern` | Truecolor, Unicode glyphs, textured rock, shimmering gems and an air gauge. Needs a modern terminal and font. |

In ASCII mode you are the lad, `p` or `q` depending on which way you face, or `g` when still.
You walk on platforms `=`, climb ladders `H`, and jump over falling boulders `o` to collect minor treasures `&` and reach the main treasure `$`.
Spikes `^` and boulders cost a life and restart the level.
Landing on a trampoline `.` bounces you off in a random direction.

The terminal must be at least 62x25 (62x24 in the modern style).

You start with 4 lads (lives).

## Controls

| Key | Action |
| --- | --- |
| ← → (or A D) | Run. You keep running until you hit something or press another key. |
| ↑ (or W) | Climb up. Press it early and you grab the next ladder you pass. |
| ↓ (or S) | Climb down, or stop if there's no ladder. |
| Space | Jump |
| P | Pause |
| Q / Esc | Quit |

Most terminals don't report key releases, so running is "press to go", not "hold to go".
In terminals that support the Kitty keyboard protocol, releasing the key stops you.

## Levels

Levels are plain text files in `src/main/resources/levels/`, listed in order in `index.txt`.
Each is 60x20 below a short header:

```
name: The Upper Gallery
time: 90      # seconds of air
carts: 4      # most carts at once
spawn: 60     # average ticks between carts (20 ticks a second)
---
<map>
```

| Map char | Meaning |
| --- | --- |
| `#` | Rock |
| `\|` | Ladder: from the lower standing row, through the floor above, to the standing row above that |
| `*` | Gem |
| `E` | Exit lift |
| `v` | Cart chute |
| `u` | Pit that swallows carts |
| `^` | Spikes (deadly) |
| `.` | Trampoline: landing on it bounces you in a random direction |
| `@` | Player start |
