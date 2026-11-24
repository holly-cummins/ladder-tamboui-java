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

    private static final String USAGE = """
            Usage: ladder [--style=ascii|modern]

              --style=ascii   green-screen ASCII; works anywhere (default)
              --style=modern  truecolor, Unicode glyphs and animation

            You can also switch style on the title screen with the arrow keys or Tab.
            """;

    private final Game game = new Game(Level.loadAll(), new Random());
    private final Renderer renderer;

    private Main(Theme theme) {
        this.renderer = new Renderer(theme);
    }

    public static void main(String[] args) throws Exception {
        Theme theme;
        try {
            theme = parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.print(USAGE);
            System.exit(2);
            return;
        }
        if (theme == null) {
            System.out.print(USAGE);
            return;
        }
        new Main(theme).run();
    }

    /** Returns the chosen theme, or null if help was asked for. */
    static Theme parseArgs(String[] args) {
        String style = "ascii";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-h") || arg.equals("--help")) {
                return null;
            } else if (arg.startsWith("--style=")) {
                style = arg.substring("--style=".length());
            } else if (arg.equals("--style") && i + 1 < args.length) {
                style = args[++i];
            } else {
                throw new IllegalArgumentException("Unknown option '" + arg + "'.");
            }
        }
        return Theme.named(style);
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
            if (game.state() == Game.State.TITLE) {
                if (isLeft(key)) {
                    renderer.cycleTheme(-1);
                    return true;
                }
                if (isRight(key) || key.isKey(KeyCode.TAB)) {
                    renderer.cycleTheme(1);
                    return true;
                }
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
