package dev.holly.ladder;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;

/**
 * 24-bit colour, Unicode glyphs, textured rock, shimmering treasure and a time gauge. Needs a terminal with
 * truecolor and a font that has box-drawing and geometric shapes.
 */
public final class ModernTheme implements Theme {

    private static final Color CAVERN = Color.rgb(18, 14, 13);
    private static final Color[] ROCK_SHADES = {
            Color.rgb(92, 64, 44), Color.rgb(99, 69, 47), Color.rgb(86, 60, 42), Color.rgb(104, 73, 50)};
    private static final Color ROCK_TOP = Color.rgb(176, 132, 84);
    private static final Color SIGN_WOOD = Color.rgb(122, 74, 36);

    private static final Style BASE = Style.EMPTY.bg(CAVERN);
    private static final Style LADDER = BASE.fg(Color.rgb(226, 190, 96));
    private static final Style EXIT = BASE.fg(Color.rgb(255, 200, 60)).bold();
    private static final Style DISPENSER = BASE.fg(Color.rgb(140, 140, 150));
    private static final Style PIT = BASE.fg(Color.rgb(80, 74, 70));
    private static final Style PIPE = BASE.fg(Color.rgb(140, 160, 178)).bold();
    private static final Style SPIKES = BASE.fg(Color.rgb(200, 208, 222)).bold();
    private static final Style TRAMPOLINE = BASE.fg(Color.rgb(255, 120, 200)).bold();
    private static final Style BOULDER = BASE.fg(Color.rgb(168, 168, 172)).bold();
    private static final Style PLAYER = BASE.fg(Color.rgb(90, 240, 255)).bold();
    private static final Style LABEL = Style.EMPTY.fg(Color.rgb(150, 140, 130));
    private static final Style VALUE = Style.EMPTY.fg(Color.rgb(255, 214, 102)).bold();
    private static final Style HEART = Style.EMPTY.fg(Color.rgb(255, 90, 110));
    private static final Style HEART_LOST = Style.EMPTY.fg(Color.rgb(80, 60, 64));
    private static final Style PLAIN = Style.EMPTY.fg(Color.rgb(225, 218, 208));
    private static final Style ACCENT = Style.EMPTY.fg(Color.rgb(120, 230, 140)).bold();
    private static final Color BORDER = Color.rgb(132, 96, 60);

    private static final int GAUGE_WIDTH = 24;

    @Override
    public String name() {
        return "modern";
    }

    @Override
    public Cell tile(Game game, int x, int y) {
        long t = game.ticks();
        return switch (game.tile(x, y)) {
            case ROCK -> rock(game, x, y);
            case LADDER -> new Cell("╫", LADDER);
            case TREASURE -> new Cell("◆", BASE.fg(shimmer(t, x, y)).bold());
            case EXIT -> new Cell("♛", BASE.fg(glint(t)).bold());
            case DISPENSER -> new Cell("▼", DISPENSER);
            case PIT -> new Cell("✺", BASE.fg(lerp(new int[]{150, 90, 255}, new int[]{230, 170, 255},
                    (Math.sin((t + x * 7) / 5.0) + 1) / 2)).bold());
            case SPIKES -> new Cell("▲", SPIKES);
            case WALL -> new Cell("┃", PIPE);
            case TRAMPOLINE -> new Cell("◡", TRAMPOLINE);
            case EMPTY -> new Cell(" ", BASE);
        };
    }

    /** Rock is solid colour with a little per-cell texture; exposed tops get a lit ledge. */
    private static Cell rock(Game game, int x, int y) {
        Color shade = ROCK_SHADES[Math.floorMod(x * 7 + y * 13 + (x * y) % 5, ROCK_SHADES.length)];
        Tile above = game.tile(x, y - 1);
        if (y > 0 && above != Tile.ROCK) {
            return new Cell("▀", Style.EMPTY.fg(ROCK_TOP).bg(shade));
        }
        return new Cell(" ", Style.EMPTY.bg(shade));
    }

    /** The crown: deep gold, catching the light now and then. */
    private static Color glint(long t) {
        double phase = Math.max(0, Math.sin(t / 3.0)) * Math.max(0, Math.sin(t / 17.0));
        return lerp(new int[]{255, 190, 40}, new int[]{255, 250, 220}, phase);
    }

    private static Color shimmer(long t, int x, int y) {
        double phase = Math.sin((t + x * 5 + y * 3) / 4.0);
        return lerp(new int[]{80, 200, 255}, new int[]{210, 250, 255}, (phase + 1) / 2);
    }

    @Override
    public Cell boulder(Game game, Game.Boulder boulder) {
        return new Cell("●", BOULDER);
    }

    /** A wooden plaque with rounded ends and cream lettering. */
    @Override
    public Cell[] sign(String name) {
        Cell[] cells = new Cell[name.length() + 4];
        Style end = BASE.fg(SIGN_WOOD);
        Style face = Style.EMPTY.fg(Color.rgb(255, 240, 200)).bg(SIGN_WOOD).bold();
        cells[0] = new Cell("▐", end);
        cells[1] = new Cell(" ", face);
        for (int i = 0; i < name.length(); i++) {
            cells[i + 2] = new Cell(String.valueOf(name.charAt(i)), face);
        }
        cells[cells.length - 2] = new Cell(" ", face);
        cells[cells.length - 1] = new Cell("▌", end);
        return cells;
    }

    @Override
    public Cell chain() {
        return new Cell("╎", BASE.fg(Color.rgb(150, 150, 160)));
    }

    @Override
    public Cell player(Game game) {
        if (game.state() == Game.State.DYING) {
            boolean flicker = (game.ticks() / 4) % 2 == 0;
            return new Cell(flicker ? "✶" : "☻", BASE.fg(flicker ? Color.rgb(255, 170, 60) : Color.rgb(255, 60, 60)).bold());
        }
        Game.Player p = game.player();
        String glyph = p.isClimbing() ? "☻" : switch (p.facing()) {
            case 1 -> "▶";
            case -1 -> "◀";
            default -> "☻";
        };
        return new Cell(glyph, PLAYER);
    }

    @Override
    public Cell particle(Game.EffectKind kind, int age) {
        double fade = Math.min(1.0, age / (double) Game.EFFECT_TICKS);
        return switch (kind) {
            case SWALLOW -> new Cell(age < 5 ? "✦" : "·",
                    BASE.fg(lerp(new int[]{235, 190, 255}, new int[]{70, 40, 90}, fade)).bold());
            case SPARKLE -> new Cell(age % 4 < 2 ? "✧" : "✦",
                    BASE.fg(lerp(new int[]{240, 255, 255}, new int[]{60, 120, 150}, fade)).bold());
        };
    }

    @Override
    public Cell curtain() {
        return new Cell("▒", BASE.fg(Color.rgb(64, 48, 40)));
    }

    @Override
    public Block.Builder fieldFrame() {
        return Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderColor(BORDER)
                .style(BASE);
    }

    @Override
    public Block.Builder bannerFrame() {
        return Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.DOUBLE)
                .borderStyle(VALUE)
                .style(Style.EMPTY.bg(Color.rgb(40, 30, 24)));
    }

    /** A time gauge that drains from green through amber to red. */
    @Override
    public Line fieldFooter(Game game) {
        if (game.level() == null || game.state() == Game.State.TITLE) {
            return null;
        }
        double left = Math.max(0, Math.min(1, game.secondsLeft() / (double) game.level().timeLimit()));
        int filled = (int) Math.ceil(left * GAUGE_WIDTH);
        int[] colour = left > 0.5
                ? mix(new int[]{240, 200, 70}, new int[]{90, 220, 110}, (left - 0.5) * 2)
                : mix(new int[]{240, 70, 60}, new int[]{240, 200, 70}, left * 2);
        Style bar = Style.EMPTY.fg(Color.rgb(colour[0], colour[1], colour[2]));
        Style empty = Style.EMPTY.fg(Color.rgb(60, 50, 44));
        List<Span> spans = new ArrayList<>();
        spans.add(Span.styled(" time ", LABEL));
        spans.add(Span.styled("━".repeat(filled), bar));
        spans.add(Span.styled("━".repeat(GAUGE_WIDTH - filled), empty));
        spans.add(Span.styled(String.format(" %3ds ", game.secondsLeft()), left < 0.2 ? bar.bold() : VALUE));
        return Line.from(spans);
    }

    @Override
    public Line infoLine(Game game) {
        if (game.level() == null) {
            return Line.from(Span.styled(" best ", LABEL), Span.styled(String.valueOf(game.highScore()), VALUE));
        }
        int lives = Math.max(0, game.lives());
        int shown = Math.min(Game.STARTING_LIVES, lives);
        return Line.from(
                Span.styled(" lads ", LABEL),
                Span.styled("♥".repeat(shown), HEART),
                Span.styled("♥".repeat(Game.STARTING_LIVES - shown), HEART_LOST),
                Span.styled("   level ", LABEL), Span.styled(String.valueOf(game.levelNumber()), VALUE),
                Span.styled(game.level().hasSign() ? "" : "  " + game.level().name(), VALUE),
                Span.styled("   ◆ ", LABEL), Span.styled(String.valueOf(game.score()), VALUE),
                Span.styled("   best ", LABEL), Span.styled(String.valueOf(game.highScore()), VALUE));
    }

    @Override
    public String helpText(Game game) {
        return switch (game.state()) {
            case TITLE -> " ␣ start   s ←→ style   q quit";
            case GAME_OVER -> " ␣ start   q quit";
            case THE_END -> " any key quits";
            case PAUSED -> " p or ␣ resume   q quit";
            case HOW_TO_PLAY -> " any key starts   q quit";
            default -> " ←→ run  ↑↓ climb  ␣ jump  b stop  s style  p pause  q quit";
        };
    }

    @Override
    public List<Line> titleArt(long ticks) {
        // Two pixels per cell: pixel rows 0-9 are letters, 10 is the shadow's last row, 11 the ledge.
        int rows = (Banner.PIXEL_HEIGHT + 2) / 2;
        int climber = rows - 1 - (int) ((ticks / 5) % (rows + 3)); // climbs up, then off the top
        List<Line> lines = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            List<Span> spans = new ArrayList<>();
            ladderRung(spans, r == climber);
            spans.add(Span.styled("  ", BASE));
            for (int x = 0; x <= Banner.PIXEL_WIDTH; x++) {
                Color top = titlePixel(x, 2 * r, ticks);
                Color bottom = titlePixel(x, 2 * r + 1, ticks);
                spans.add(top == CAVERN && bottom == CAVERN
                        ? Span.styled(" ", BASE)
                        : Span.styled("▀", Style.EMPTY.fg(top).bg(bottom)));
            }
            spans.add(Span.styled("  ", BASE));
            ladderRung(spans, false);
            lines.add(Line.from(spans));
        }
        return lines;
    }

    private static void ladderRung(List<Span> spans, boolean withClimber) {
        spans.add(Span.styled("╟", LADDER));
        spans.add(withClimber ? Span.styled("☻", PLAYER) : Span.styled("─", LADDER));
        spans.add(Span.styled("╢", LADDER));
    }

    /**
     * One pixel of the title: gold-to-copper letters with a glint that sweeps across every few seconds,
     * a drop shadow, and a lit rock ledge for the letters to stand on.
     */
    private static Color titlePixel(int x, int y, long ticks) {
        if (Banner.pixel(x, y)) {
            int[] colour = mix(new int[]{255, 234, 150}, new int[]{205, 98, 38}, y / (double) (Banner.PIXEL_HEIGHT - 1));
            int distance = Math.abs(x - y / 2 - ((int) (ticks % 100) - 20));
            if (distance <= 2) {
                colour = mix(colour, new int[]{255, 255, 255}, (3 - distance) * 0.25);
            }
            return Color.rgb(colour[0], colour[1], colour[2]);
        }
        if (Banner.pixel(x - 1, y - 1)) {
            return Color.rgb(70, 40, 24);
        }
        if (y == Banner.PIXEL_HEIGHT + 1) {
            return ROCK_TOP;
        }
        return CAVERN;
    }

    @Override
    public List<String> blurb() {
        return List.of(
                "Reach the treasure before time runs out.",
                "Jump the rolling boulders. Grab the treasure on the way.");
    }

    @Override
    public List<LegendEntry> legend() {
        return List.of(
                new LegendEntry(new Cell("▶", PLAYER), "you (◀ left, ☻ still)"),
                new LegendEntry(new Cell("●", BOULDER), "boulder"),
                new LegendEntry(new Cell("◆", BASE.fg(Color.rgb(120, 220, 255)).bold()), "treasure, " + Game.TREASURE_POINTS + " points"),
                new LegendEntry(new Cell("┃", PIPE), "pipe, blocks your way"),
                new LegendEntry(new Cell("✺", BASE.fg(Color.rgb(190, 130, 255)).bold()), "star, swallows boulders"),
                new LegendEntry(new Cell("▲", SPIKES), "spikes, deadly"),
                new LegendEntry(new Cell("◡", TRAMPOLINE), "trampoline, bounces you"),
                new LegendEntry(new Cell("♛", EXIT), "the crown, next level"),
                new LegendEntry(new Cell("╫", LADDER), "ladder"));
    }

    @Override
    public Style label() {
        return LABEL;
    }

    @Override
    public Style value() {
        return VALUE;
    }

    @Override
    public Style plain() {
        return PLAIN;
    }

    @Override
    public Style accent() {
        return ACCENT;
    }

    private static Color lerp(int[] a, int[] b, double f) {
        int[] c = mix(a, b, f);
        return Color.rgb(c[0], c[1], c[2]);
    }

    /** Linear blend from a (f = 0) to b (f = 1). */
    private static int[] mix(int[] a, int[] b, double f) {
        int[] c = new int[3];
        for (int i = 0; i < 3; i++) {
            c[i] = (int) Math.round(a[i] + (b[i] - a[i]) * f);
        }
        return c;
    }
}
