package dev.holly.ladder;

import java.time.Duration;
import java.util.Random;

import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;

/** Wires the game to the terminal: TamboUI's TuiRunner supplies ticks, keys and redraws. */
public final class Main {

    private final Game game = new Game(Level.loadAll(), new Random());
    private final Renderer renderer = new Renderer();

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    private void run() throws Exception {
        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(1000 / Game.TICKS_PER_SECOND))
                .build();
        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(this::handle, frame -> renderer.draw(frame, game));
        }
    }

    private boolean handle(Event event, TuiRunner runner) {
        if (event instanceof TickEvent) {
            game.tick();
            return true;
        }
        if (event instanceof KeyEvent key) {
            if (key.isQuit() || key.isKey(KeyCode.ESCAPE)) {
                runner.quit();
                return false;
            }
            if (key.isRelease()) {
                // Only terminals speaking the Kitty keyboard protocol send these.
                if (isLeft(key)) {
                    game.stopHorizontal(-1);
                } else if (isRight(key)) {
                    game.stopHorizontal(1);
                }
                return false;
            }
            if (isLeft(key)) {
                game.left();
            } else if (isRight(key)) {
                game.right();
            } else if (key.isKey(KeyCode.UP) || key.isCharIgnoreCase('w')) {
                game.up();
            } else if (key.isKey(KeyCode.DOWN) || key.isCharIgnoreCase('s')) {
                game.down();
            } else if (key.isChar(' ') || key.isKey(KeyCode.ENTER)) {
                game.confirm();
            } else if (key.isCharIgnoreCase('p')) {
                game.togglePause();
            }
            return true;
        }
        return event instanceof ResizeEvent;
    }

    private static boolean isLeft(KeyEvent key) {
        return key.isKey(KeyCode.LEFT) || key.isCharIgnoreCase('a');
    }

    private static boolean isRight(KeyEvent key) {
        return key.isKey(KeyCode.RIGHT) || key.isCharIgnoreCase('d');
    }
}
