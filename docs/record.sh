#!/usr/bin/env bash
# Re-records the README animations: the self-playing demo, once in each style.
# Uses TamboUI's built-in asciinema recorder, then agg (https://github.com/asciinema/agg) for the GIFs.
# Needs: Maven, tmux, agg, and a monospace font with good Unicode coverage (DejaVu Sans Mono).
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -q package -DskipTests
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Black background and phosphor green, so the ASCII style looks like a green screen. Modern uses
# truecolour, so the palette doesn't touch it. Format: background, foreground, then the 16 ANSI colours.
palette="000000,c0c0c0,000000,cd3131,33dd55,e5e510,2472c8,bc3fbc,11a8cd,e5e5e5,666666,f14c4c,66ff88,f5f543,3b8eea,d670d6,29b8db,ffffff"

for style in ascii modern; do
    cast="$work/ladder-$style.cast"
    # The game needs a real terminal to run in; a detached tmux session gives it one, 64x24.
    tmux new-session -d -s "ladder-record-$style" -x 64 -y 24 \
        "java -Dtamboui.record=$cast -Dtamboui.record.width=64 -Dtamboui.record.height=24 \
              -Dtamboui.record.fps=20 -Dtamboui.record.duration=13000 \
              --enable-native-access=ALL-UNNAMED -jar target/ladder-0.1.0-SNAPSHOT.jar --demo --style=$style; \
         tmux wait-for -S ladder-recorded-$style"
    tmux wait-for "ladder-recorded-$style"
    agg --quiet --font-family "DejaVu Sans Mono" --font-size 16 --fps-cap 20 --theme "$palette" \
        "$cast" "docs/ladder-$style.gif"
    echo "docs/ladder-$style.gif"
done
