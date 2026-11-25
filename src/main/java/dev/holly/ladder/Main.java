package dev.holly.ladder;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;

/** Wires the game to the terminal: TamboUI's TuiRunner supplies ticks, keys and redraws. */
public final class Main {

    private static final String USAGE = """
            Usage: ladder [--style=ascii|modern] [--sound]

              --style=ascii   green-screen ASCII; works anywhere (default)
              --style=modern  truecolor, Unicode glyphs and animation
              --sound         ring the terminal bell when you lose a lad

            You can also switch style at any time with s, or on the title screen with the arrow keys or Tab.
            """;

    private final List<Level> levels = Level.loadAll();
    private final Game game = new Game(levels, new Random());
    private final AttractMode attract = new AttractMode(levels);
    private final Renderer renderer;
    private final boolean sound;
    private Game.State lastState;
    private String windowTitle = "";
    private boolean mouseCaptured;

    /** What the command line asked for. */
    record Options(Theme theme, boolean sound) {}

    private Main(Options options) {
        this.renderer = new Renderer(options.theme());
        this.sound = options.sound();
    }

    public static void main(String[] args) throws Exception {
        Options options;
        try {
            options = parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.print(USAGE);
            System.exit(2);
            return;
        }
        if (options == null) {
            System.out.print(USAGE);
            return;
        }
        new Main(options).run();
    }

    /** Returns the chosen options, or null if help was asked for. */
    static Options parseArgs(String[] args) {
        String style = "ascii";
        boolean sound = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-h") || arg.equals("--help")) {
                return null;
            } else if (arg.startsWith("--style=")) {
                style = arg.substring("--style=".length());
            } else if (arg.equals("--style") && i + 1 < args.length) {
                style = args[++i];
            } else if (arg.equals("--sound")) {
                sound = true;
            } else {
                throw new IllegalArgumentException("Unknown option '" + arg + "'.");
            }
        }
        return new Options(Theme.named(style), sound);
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
            updateWindowTitle(tui);
            tui.draw(this::draw);
            try {
                tui.run(this::handle, this::draw);
            } finally {
                captureMouse(tui, false);
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

    private void draw(Frame frame) {
        if (attract.running()) {
            renderer.draw(frame, attract.game(),
                    attract.showQuiche() ? " You eat quiche!" : " DEMO   press any key to play");
        } else {
            renderer.draw(frame, game);
        }
    }

    private boolean handle(Event event, TuiRunner runner) {
        boolean redraw = react(event, runner);
        ringOnLosingALad(runner);
        updateWindowTitle(runner);
        captureMouse(runner, attract.running() || game.state() == Game.State.TITLE);
        return redraw;
    }

    /** With --sound, the terminal bell rings as a lad is lost (not in the demo, which isn't yours to lose). */
    private void ringOnLosingALad(TuiRunner tui) {
        Game.State state = game.state();
        if (sound && state == Game.State.DYING && lastState != Game.State.DYING) {
            try {
                tui.backend().writeRaw("\u0007");
                tui.backend().flush();
            } catch (java.io.IOException e) {
                // no bell, then
            }
        }
        lastState = state;
    }

    /**
     * Mouse clicks only do something on the title screen and in the demo. Everywhere else the terminal
     * keeps the mouse, so links on the how-to screen open and text can be selected as usual.
     */
    private void captureMouse(TuiRunner tui, boolean capture) {
        if (capture == mouseCaptured) {
            return;
        }
        mouseCaptured = capture;
        try {
            if (capture) {
                tui.backend().enableMouseCapture();
            } else {
                tui.backend().disableMouseCapture();
            }
            tui.backend().flush();
        } catch (java.io.IOException e) {
            // no mouse, then; the keyboard still works
        }
    }

    /** A click on the title screen picks a style or starts; in the demo it hands back the controls. */
    private boolean click(MouseEvent mouse) {
        if (!mouse.isPress() || !mouse.isLeftButton()) {
            return false;
        }
        attract.resetIdle();
        if (attract.running()) {
            attract.stop();
            return true;
        }
        if (game.state() != Game.State.TITLE) {
            return false;
        }
        String action = renderer.clickAt(mouse.x(), mouse.y());
        if (action == null) {
            return false;
        }
        if (action.equals("start")) {
            game.confirm();
        } else if (action.startsWith("style:")) {
            renderer.selectTheme(action.substring("style:".length()));
        }
        return true;
    }

    /** Names what's on screen in the terminal's title bar; the terminal's own title comes back on exit. */
    private void updateWindowTitle(TuiRunner tui) {
        String title;
        if (attract.running()) {
            title = "Ladder · demo";
        } else if (game.level() == null || game.state() == Game.State.TITLE || game.state() == Game.State.HOW_TO_PLAY) {
            title = "Ladder";
        } else {
            title = "Ladder · " + game.level().name();
        }
        if (!title.equals(windowTitle)) {
            windowTitle = title;
            tui.setWindowTitle(title);
        }
    }

    private boolean react(Event event, TuiRunner runner) {
        if (event instanceof TickEvent) {
            if (attract.running()) {
                attract.tick();
            } else {
                game.tick();
                if (game.state() == Game.State.TITLE) {
                    attract.idleTick();
                } else {
                    attract.resetIdle();
                }
            }
            return true;
        }
        if (event instanceof MouseEvent mouse) {
            return click(mouse);
        }
        if (event instanceof KeyEvent key) {
            if (key.isQuit() || key.isKey(KeyCode.ESCAPE)) {
                runner.quit();
                return false;
            }
            if (key.isRelease()) {
                return false; // one press is enough: letting go of a key changes nothing
            }
            attract.resetIdle();
            if (key.isCharIgnoreCase('s')) {
                renderer.cycleTheme(1);
                return true;
            }
            if (attract.running()) {
                attract.stop(); // any other key ends the demo, back to the title
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
