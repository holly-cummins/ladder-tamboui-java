# Ladder

A terminal platform game written in Java with [TamboUI](https://tamboui.dev).
Reach the treasure before time runs out, jumping the rolling boulders and grabbing the loot on the way.

## Build and run

Needs Java 25 and Maven.

```sh
mvn package
java --enable-native-access=ALL-UNNAMED -jar target/ladder-0.1.0-SNAPSHOT.jar
```

There are two looks. Press S to switch between them at any time (← → and Tab also work on the title screen), or pick the starting one with `--style`:

| Option | Look |
| --- | --- |
| `--style=ascii` (default) | Plain 7-bit ASCII, green on black like a classic green screen. Works in any terminal. |
| `--style=modern` | Truecolor, Unicode glyphs, textured rock, shimmering treasure and an air gauge. Needs a modern terminal and font. |

In ASCII mode you are the lad, `p` or `q` depending on which way you face, or `g` when still.
You walk on platforms `=`, climb ladders `H`, and jump over rolling boulders `o` to collect minor treasures `&` and reach the main treasure `$`.
Pipes `|` block your way. Spikes `^` and boulders cost a life and restart the level.
Landing on a trampoline `.` bounces you off in a random direction.
Boulders pour out of chutes `V` and are swallowed by stars `*`.

In modern mode you are a cyan `▶` or `◀` depending on which way you face, or `☻` when still or climbing.
Boulders are grey `●`, ladders `╫`, pipes `┃`, spikes `▲`, trampolines `◡`, stars `✺` and the exit `⌂`.

The terminal must be at least 62x24.

You start with 4 lads (lives).

## Controls

| Key | Action |
| --- | --- |
| ← → (or A D) | Run. You keep running until you hit something or press another key. |
| ↑ (or W) | Climb up. Press it early and you grab the next ladder you pass, even in mid-jump. |
| ↓ | Climb down, or stop if there's no ladder. |
| Space | Jump |
| P | Pause |
| S | Switch style (ASCII or modern), on the title screen or mid-game |
| Q / Esc | Quit |

Most terminals don't report key releases, so running is "press to go", not "hold to go".
In terminals that support the Kitty keyboard protocol, releasing the key stops you.

To get onto a ladder that doesn't reach the floor, jump at it and press ↑ just before or during the jump.
You can reach one cell ahead, so the jump only has to come close.

## Levels

Levels are plain text files in `src/main/resources/levels/`, listed in order in `index.txt`.
Each is 60x20 below a short header:

```
name: Easy Street
time: 90      # seconds on the clock
boulders: 4    # most boulders at once
spawn: 60     # average ticks between boulders (20 ticks a second)
---
<map>
```

| Map char | Meaning |
| --- | --- |
| `#` | Rock |
| `H` | Ladder: from the lower standing row, through the floor above, to the standing row above that |
| `\|` | Pipe wall: blocks movement like rock. A one-high pipe is an easy jump; two high needs a perfectly timed running jump. |
| `&` | Treasure |
| `E` | Exit lift |
| `v` | Boulder chute. Boulders roll away from a `\|` pipe beside it, and after that only follow gravity and momentum. |
| `*` | Star that swallows boulders |
| `^` | Spikes (deadly). Good on top of pipes, or under gaps to punish a fall. |
| `.` | Trampoline: landing on it bounces you in a random direction. Put them in pairs, either side of a gap in the floor. |
| `@` | Player start |
| `[` | Left end of the sign showing the level name. It hangs on chains from whatever is above it. |

Every level in `index.txt` is checked by `LevelSolverTest`, which searches all inputs (with boulders off) to prove the level can be finished in time.
