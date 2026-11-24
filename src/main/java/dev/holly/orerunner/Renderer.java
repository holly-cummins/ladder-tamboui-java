package dev.holly.orerunner;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

/** Draws a {@link Game} into a TamboUI frame. */
public final class Renderer {

    private static final int FIELD_W = Level.WIDTH + 2;   // plus border
    private static final int FIELD_H = Level.HEIGHT + 2;
    private static final int TOTAL_H = FIELD_H + 2;        // plus status and help lines

    private static final Style ROCK = Style.EMPTY.fg(Color.rgb(150, 105, 60));
    private static final Style LADDER = Style.EMPTY.fg(Color.rgb(220, 190, 90));
    private static final Style GEM = Style.EMPTY.fg(Color.LIGHT_CYAN).bold();
    private static final Style EXIT = Style.EMPTY.fg(Color.LIGHT_GREEN).bold();
    private static final Style CHUTE = Style.EMPTY.fg(Color.GRAY);
    private static final Style PIT = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style GAS_A = Style.EMPTY.fg(Color.GREEN);
    private static final Style GAS_B = Style.EMPTY.fg(Color.LIGHT_GREEN).dim();
    private static final Style CART = Style.EMPTY.fg(Color.LIGHT_RED).bold();
    private static final Style PLAYER = Style.EMPTY.fg(Color.BRIGHT_WHITE).bold();
    private static final Style DEAD = Style.EMPTY.fg(Color.RED).bold();
    private static final Style PLAIN = Style.EMPTY;
    private static final Style LABEL = Style.EMPTY.fg(Color.GRAY);
    private static final Style VALUE = Style.EMPTY.fg(Color.LIGHT_YELLOW).bold();
    private static final Style BORDER = Style.EMPTY.fg(Color.rgb(110, 80, 50));

    public void draw(Frame frame, Game game) {
        Rect area = frame.area();
        if (area.width() < FIELD_W || area.height() < TOTAL_H) {
            frame.renderWidget(Paragraph.builder()
                    .text(Text.from(
                            Line.from("Ore Runner needs a terminal of at least " + FIELD_W + "x" + TOTAL_H + "."),
                            Line.from("This one is " + area.width() + "x" + area.height() + ". Press q to quit.")))
                    .build(), area);
            return;
        }

        Rect column = centre(area, FIELD_W, TOTAL_H);
        List<Rect> rows = Layout.vertical()
                .constraints(Constraint.length(1), Constraint.length(FIELD_H), Constraint.length(1))
                .split(column);

        frame.renderWidget(Paragraph.from(statusLine(game)), rows.get(0));
        Block block = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.THICK)
                .borderStyle(BORDER)
                .title(Title.from(Line.from(Span.styled(" ORE RUNNER ", VALUE))).centered())
                .build();

        if (game.state() == Game.State.TITLE) {
            frame.renderWidget(Paragraph.builder().text(titleText()).block(block).centered().build(), rows.get(1));
        } else {
            frame.renderWidget(Paragraph.builder().text(field(game)).block(block).build(), rows.get(1));
            String banner = banner(game);
            if (banner != null) {
                drawBanner(frame, rows.get(1), banner);
            }
        }
        frame.renderWidget(Paragraph.from(Line.styled(helpText(game), LABEL)), rows.get(2));
    }

    private static Rect centre(Rect area, int w, int h) {
        return new Rect(area.x() + (area.width() - w) / 2, area.y() + (area.height() - h) / 2, w, h);
    }

    private Line statusLine(Game game) {
        if (game.level() == null) {
            return Line.from(Span.styled(" Best ", LABEL), Span.styled(String.valueOf(game.highScore()), VALUE));
        }
        return Line.from(
                Span.styled(" " + game.levelNumber() + ": ", LABEL), Span.styled(game.level().name(), VALUE),
                Span.styled("  Score ", LABEL), Span.styled(String.valueOf(game.score()), VALUE),
                Span.styled("  Lives ", LABEL), Span.styled(String.valueOf(Math.max(0, game.lives())), VALUE),
                Span.styled("  Air ", LABEL), Span.styled(String.valueOf(game.secondsLeft()), VALUE),
                Span.styled("  Best ", LABEL), Span.styled(String.valueOf(game.highScore()), VALUE));
    }

    private String helpText(Game game) {
        return switch (game.state()) {
            case TITLE, GAME_OVER -> " space: start    q: quit";
            case PAUSED -> " p or space: resume    q: quit";
            default -> " ←→ run  ↑↓ climb  ↓ stop  space jump  p pause  q quit";
        };
    }

    private String banner(Game game) {
        return switch (game.state()) {
            case PAUSED -> "PAUSED";
            case DYING -> game.secondsLeft() <= 0 && game.lives() > 0 ? "OUT OF AIR" : null;
            case LEVEL_DONE -> "LEVEL CLEAR";
            case GAME_OVER -> "GAME OVER";
            default -> null;
        };
    }

    private void drawBanner(Frame frame, Rect field, String text) {
        int w = text.length() + 6;
        Rect r = centre(field, w, 3);
        frame.renderWidget(Paragraph.builder()
                .text(Text.from(Line.from(Span.styled(text, VALUE))))
                .centered()
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.DOUBLE).borderStyle(VALUE).build())
                .build(), r);
    }

    private Text titleText() {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.empty());
        lines.add(Line.from(Span.styled("O R E   R U N N E R", VALUE)));
        lines.add(Line.empty());
        lines.add(Line.styled("Climb out of the mine before your air runs out.", PLAIN));
        lines.add(Line.styled("Dodge the runaway ore carts. Grab gems on the way.", PLAIN));
        lines.add(Line.empty());
        lines.add(legend("@", PLAYER, "you"));
        lines.add(legend("●", CART, "runaway cart"));
        lines.add(legend("◆", GEM, "gem, " + Game.GEM_POINTS + " points"));
        lines.add(legend("≈", GAS_A, "gas vent, deadly"));
        lines.add(legend("⌂", EXIT, "lift to the next level"));
        lines.add(legend("╫", LADDER, "ladder"));
        lines.add(Line.empty());
        lines.add(Line.styled("Press space to start", EXIT));
        return Text.from(lines);
    }

    private static Line legend(String glyph, Style style, String what) {
        return Line.from(Span.styled(glyph, style), Span.styled("  " + String.format("%-24s", what), LABEL));
    }

    private Text field(Game game) {
        String[] chars = new String[Level.WIDTH];
        Style[] styles = new Style[Level.WIDTH];
        boolean flicker = (game.ticks() / 4) % 2 == 0;
        List<Line> lines = new ArrayList<>(Level.HEIGHT);

        for (int y = 0; y < Level.HEIGHT; y++) {
            for (int x = 0; x < Level.WIDTH; x++) {
                switch (game.tile(x, y)) {
                    case ROCK -> set(chars, styles, x, "▒", ROCK);
                    case LADDER -> set(chars, styles, x, "╫", LADDER);
                    case GEM -> set(chars, styles, x, "◆", GEM);
                    case EXIT -> set(chars, styles, x, "⌂", EXIT);
                    case CHUTE -> set(chars, styles, x, "▼", CHUTE);
                    case PIT -> set(chars, styles, x, "◡", PIT);
                    case GAS -> set(chars, styles, x, flicker ? "≈" : "~", flicker ? GAS_A : GAS_B);
                    default -> set(chars, styles, x, " ", PLAIN);
                }
            }
            for (Game.Cart c : game.carts()) {
                if (c.y() == y && c.x() >= 0 && c.x() < Level.WIDTH) {
                    set(chars, styles, c.x(), "●", CART);
                }
            }
            Game.Player p = game.player();
            if (p.y() == y) {
                if (game.state() == Game.State.DYING) {
                    set(chars, styles, p.x(), flicker ? "✶" : "@", DEAD);
                } else {
                    set(chars, styles, p.x(), "@", PLAYER);
                }
            }
            lines.add(toLine(chars, styles));
        }
        return Text.from(lines);
    }

    private static void set(String[] chars, Style[] styles, int x, String c, Style s) {
        chars[x] = c;
        styles[x] = s;
    }

    /** Merges runs of same-styled cells into single spans. */
    private static Line toLine(String[] chars, Style[] styles) {
        List<Span> spans = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style current = styles[0];
        for (int x = 0; x < chars.length; x++) {
            if (styles[x] != current) {
                spans.add(Span.styled(run.toString(), current));
                run.setLength(0);
                current = styles[x];
            }
            run.append(chars[x]);
        }
        spans.add(Span.styled(run.toString(), current));
        return Line.from(spans);
    }
}
