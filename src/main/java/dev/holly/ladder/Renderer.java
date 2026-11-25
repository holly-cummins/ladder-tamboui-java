package dev.holly.ladder;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

/** Draws a {@link Game} into a TamboUI frame, using a {@link Theme} for how things look. */
public final class Renderer {

    private static final int FIELD_W = Level.WIDTH + 2;   // plus border
    private static final int FIELD_H = Level.HEIGHT + 2;

    static final String PROJECT_URL = "https://github.com/holly-cummins/ladder-tamboui-java";
    static final String TAMBOUI_URL = "https://tamboui.dev";

    private Theme theme;

    public Renderer(Theme theme) {
        this.theme = theme;
    }

    public Theme theme() {
        return theme;
    }

    /** Moves to the next (+1) or previous (-1) style in {@link Theme#NAMES}, wrapping round. */
    public void cycleTheme(int step) {
        int i = Theme.NAMES.indexOf(theme.name());
        theme = Theme.named(Theme.NAMES.get(Math.floorMod(i + step, Theme.NAMES.size())));
    }

    public void draw(Frame frame, Game game) {
        draw(frame, game, null);
    }

    /** Draws the game, with {@code bottomText} (if not null) in place of the usual help line. */
    public void draw(Frame frame, Game game, String bottomText) {
        Rect area = frame.area();
        frame.renderWidget(Block.builder().style(theme.screen()).build(), area);
        int totalH = FIELD_H + 2; // plus the info and help lines
        if (area.width() < FIELD_W || area.height() < totalH) {
            frame.renderWidget(Paragraph.builder()
                    .text(Text.from(
                            Line.from("Ladder needs a terminal of at least " + FIELD_W + "x" + totalH + "."),
                            Line.from("This one is " + area.width() + "x" + area.height() + ". Press q to quit.")))
                    .build(), area);
            return;
        }

        Rect column = centre(area, FIELD_W, totalH);
        List<Rect> rows = Layout.vertical()
                .constraints(Constraint.length(FIELD_H), Constraint.length(1), Constraint.length(1))
                .split(column);

        Block.Builder frameBuilder = theme.fieldFrame()
                .title(Title.from(Line.from(Span.styled(" LADDER ", theme.value().hyperlink(PROJECT_URL)))).centered());
        Line footer = theme.fieldFooter(game);
        if (footer != null) {
            frameBuilder.titleBottom(Title.from(footer).centered());
        }
        Block block = frameBuilder.build();

        if (game.state() == Game.State.TITLE) {
            frame.renderWidget(Paragraph.builder().text(titleText(game)).block(block).centered().build(), rows.get(0));
        } else if (game.state() == Game.State.HOW_TO_PLAY) {
            frame.renderWidget(Paragraph.builder().text(howToPlayText()).block(block).build(), rows.get(0));
        } else {
            frame.renderWidget(Paragraph.builder().text(field(game)).block(block).build(), rows.get(0));
            String banner = banner(game);
            if (banner != null) {
                drawBanner(frame, rows.get(0), banner);
            }
        }
        frame.renderWidget(Paragraph.from(theme.infoLine(game)), rows.get(1));
        Line bottom = bottomText != null
                ? Line.styled(bottomText, theme.accent())
                : Line.styled(theme.helpText(game), theme.label());
        frame.renderWidget(Paragraph.from(bottom), rows.get(2));
    }

    private static Rect centre(Rect area, int w, int h) {
        return new Rect(area.x() + (area.width() - w) / 2, area.y() + (area.height() - h) / 2, w, h);
    }

    private String banner(Game game) {
        return switch (game.state()) {
            case PAUSED -> "PAUSED";
            case DYING -> game.secondsLeft() <= 0 && game.lives() > 0 ? theme.outOfTimeBanner() : null;
            case LEVEL_DONE -> "LEVEL CLEAR";
            case GAME_OVER -> "GAME OVER";
            default -> null;
        };
    }

    private void drawBanner(Frame frame, Rect field, String text) {
        int w = text.length() + 6;
        Rect r = centre(field, w, 3);
        frame.renderWidget(Paragraph.builder()
                .text(Text.from(Line.from(Span.styled(text, theme.value()))))
                .centered()
                .block(theme.bannerFrame().build())
                .build(), r);
    }

    Text titleText(Game game) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.empty());
        lines.addAll(theme.titleArt(game.ticks()));
        lines.add(Line.empty());
        for (String s : theme.blurb()) {
            lines.add(Line.styled(s, theme.plain()));
        }
        lines.add(Line.empty());
        lines.addAll(legend());
        lines.add(Line.empty());
        lines.add(styleSwitcher());
        lines.add(Line.styled("Press space to start", theme.accent()));
        return Text.from(lines);
    }

    /** Shown once, between the title and the first game: one press of a key is all it takes. */
    Text howToPlayText() {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            lines.add(Line.empty());
        }
        lines.add(Line.styled("  HOW TO PLAY", theme.value()));
        lines.add(Line.empty());
        lines.add(Line.styled("  One press is enough: no need to hold or mash keys.", theme.accent()));
        lines.add(Line.styled("  The lad remembers what you asked for and gets on with it.", theme.accent()));
        lines.add(Line.empty());
        String[][] keys = {
                {theme.leftRightKeys(), "Run. You keep going until you hit something."},
                {"b", "Stop."},
                {theme.upDownKeys(), "Climb. Press it early, while running or jumping:"},
                {"", "you catch the next ladder you reach."},
                {theme.jumpKey(), "Jump. Over boulders, gaps and low pipes."},
                {"", "In mid-air, steer or stop with " + theme.leftRightKeys() + " and b."},
                {"p", "Pause.     s  Switch style.     q  Quit."},
        };
        for (String[] k : keys) {
            lines.add(Line.from(
                    Span.styled(String.format("  %-6s ", k[0]), theme.value()),
                    Span.styled(k[1], theme.plain())));
        }
        lines.add(Line.empty());
        lines.add(Line.styled("  Press any key to start", theme.accent()));
        lines.add(Line.empty());
        lines.add(link("  Made with ", TAMBOUI_URL));
        lines.add(link("  Source    ", PROJECT_URL));
        return Text.from(lines);
    }

    /** A label and a clickable link, in terminals that support OSC 8 hyperlinks. */
    private Line link(String label, String url) {
        String href = url.startsWith("http") ? url : "https://" + url;
        return Line.from(
                Span.styled(label, theme.label()),
                Span.styled(url.replace("https://", ""), theme.value().hyperlink(href).underlined()));
    }

    /** The key to the map, in two columns so the big title fits. */
    private List<Line> legend() {
        List<Theme.LegendEntry> entries = theme.legend();
        int width = 0;
        for (Theme.LegendEntry e : entries) {
            width = Math.max(width, e.meaning().length());
        }
        String format = "%-" + width + "s";
        int half = (entries.size() + 1) / 2;
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < half; i++) {
            List<Span> spans = new ArrayList<>();
            for (int j : new int[]{i, i + half}) {
                if (j > i) {
                    spans.add(Span.styled("  ", theme.label()));
                }
                Theme.LegendEntry e = j < entries.size() ? entries.get(j) : null;
                spans.add(e == null ? Span.styled(" ", theme.label()) : Span.styled(e.cell().glyph(), e.cell().style()));
                spans.add(Span.styled(" " + String.format(format, e == null ? "" : e.meaning()), theme.label()));
            }
            lines.add(Line.from(spans));
        }
        return lines;
    }

    /** Shows every style with the current one bracketed, e.g. {@code Style: [ascii]  modern}. */
    private Line styleSwitcher() {
        List<Span> spans = new ArrayList<>();
        spans.add(Span.styled("Style:", theme.label()));
        for (String name : Theme.NAMES) {
            boolean current = name.equals(theme.name());
            spans.add(Span.styled(current ? " [" + name + "]" : "  " + name + " ", current ? theme.value() : theme.label()));
        }
        return Line.from(spans);
    }

    private Text field(Game game) {
        Theme.Cell[] cells = new Theme.Cell[Level.WIDTH];
        List<Line> lines = new ArrayList<>(Level.HEIGHT);
        for (int y = 0; y < Level.HEIGHT; y++) {
            for (int x = 0; x < Level.WIDTH; x++) {
                cells[x] = theme.tile(game, x, y);
            }
            drawSign(game, cells, y);
            for (Game.Boulder c : game.boulders()) {
                if (c.y() == y && c.x() >= 0 && c.x() < Level.WIDTH) {
                    cells[c.x()] = theme.boulder(game, c);
                }
            }
            Game.Player p = game.player();
            if (p.y() == y) {
                cells[p.x()] = theme.player(game);
            }
            lines.add(toLine(cells));
        }
        return Text.from(lines);
    }

    /** Overlays the level-name sign and the chains it hangs from, if the level has one. */
    private void drawSign(Game game, Theme.Cell[] cells, int y) {
        Level level = game.level();
        if (!level.hasSign() || y > level.signY()) {
            return;
        }
        Theme.Cell[] sign = theme.sign(level.name());
        if (y == level.signY()) {
            for (int i = 0; i < sign.length && level.signX() + i < Level.WIDTH; i++) {
                cells[level.signX() + i] = sign[i];
            }
            return;
        }
        // Above the sign: chains run up from both ends until they meet something to hang from.
        for (int x : new int[] {level.signX() + 1, level.signX() + sign.length - 2}) {
            if (x < Level.WIDTH && chainReaches(game, x, y, level.signY())) {
                cells[x] = theme.chain();
            }
        }
    }

    private static boolean chainReaches(Game game, int x, int y, int signY) {
        for (int yy = y; yy < signY; yy++) {
            if (game.tile(x, yy) != Tile.EMPTY) {
                return false;
            }
        }
        return true;
    }

    /** Merges runs of same-styled cells into single spans. */
    private static Line toLine(Theme.Cell[] cells) {
        List<Span> spans = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style current = cells[0].style();
        for (Theme.Cell cell : cells) {
            if (!cell.style().equals(current)) {
                spans.add(Span.styled(run.toString(), current));
                run.setLength(0);
                current = cell.style();
            }
            run.append(cell.glyph());
        }
        spans.add(Span.styled(run.toString(), current));
        return Line.from(spans);
    }
}
