package dev.holly.ladder;

import java.time.Duration;
import java.util.Random;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
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

            You can also switch style at any time with s, or on the title screen with the arrow keys or Tab.
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
        warmUp();
        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(1000 / Game.TICKS_PER_SECOND))
                .alternateScreen(false) // switched below, in the same write as the first frame
                .build();
        try (TuiRunner tui = TuiRunner.create(config)) {
            // Switching to the alternate screen shows it blank (or, in some terminals, with the last
            // run's picture) until the first frame arrives. Begin a synchronized update first, so the
            // terminal holds the display until the title is complete; the frame's own end-of-update
            // releases it. Terminals without synchronized output just ignore it.
            tui.backend().writeRaw("\u001b[?2026h\u001b[?1049h");
            tui.draw(frame -> renderer.draw(frame, game));
            try {
                tui.run(this::handle, frame -> renderer.draw(frame, game));
            } finally {
                tui.backend().leaveAlternateScreen();
            }
        }
    }

    /**
     * Draws the title a few times off-screen before taking over the terminal, so the classes and code
     * behind the first frame are loaded and warm, and it appears the moment the screen switches.
     */
    private void warmUp() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 80, 30));
        for (int i = 0; i < 20; i++) {
            renderer.draw(Frame.forTesting(buffer), game);
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
                return false; // one press is enough: letting go of a key changes nothing
            }
            if (key.isCharIgnoreCase('s')) {
                renderer.cycleTheme(1);
                return true;
            }
            for (char c = '1'; c <= '9'; c++) {
                if (key.isChar(c) && (game.state() == Game.State.TITLE || game.state() == Game.State.HOW_TO_PLAY)) {
                    game.cheatToLevel(c - '0'); // cheat: start on a later level
                    return true;
                }
            }
            if (game.state() == Game.State.HOW_TO_PLAY) {
                game.confirm(); // any key starts the game
                return true;
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
            } else if (key.isKey(KeyCode.DOWN)) {
                game.down();
            } else if (key.isCharIgnoreCase('b')) {
                game.stop();
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
