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
        Rect area = frame.area();
        frame.renderWidget(Block.builder().style(theme.screen()).build(), area);
        Line info = theme.infoLine(game);
        int infoH = info == null ? 0 : 1;
        int totalH = FIELD_H + 2 + infoH; // plus status, info and help lines
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
                .constraints(Constraint.length(1), Constraint.length(FIELD_H), Constraint.length(infoH), Constraint.length(1))
                .split(column);

        frame.renderWidget(Paragraph.from(theme.statusLine(game)), rows.get(0));
        Block.Builder frameBuilder = theme.fieldFrame()
                .title(Title.from(Line.from(Span.styled(" LADDER ", theme.value()))).centered());
        Line footer = theme.fieldFooter(game);
        if (footer != null) {
            frameBuilder.titleBottom(Title.from(footer).centered());
        }
        Block block = frameBuilder.build();

        if (game.state() == Game.State.TITLE) {
            frame.renderWidget(Paragraph.builder().text(titleText(game)).block(block).centered().build(), rows.get(1));
        } else {
            frame.renderWidget(Paragraph.builder().text(field(game)).block(block).build(), rows.get(1));
            String banner = banner(game);
            if (banner != null) {
                drawBanner(frame, rows.get(1), banner);
            }
        }
        if (info != null) {
            frame.renderWidget(Paragraph.from(info), rows.get(2));
        }
        frame.renderWidget(Paragraph.from(Line.styled(theme.helpText(game), theme.label())), rows.get(3));
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

    private Text titleText(Game game) {
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
        lines.add(Line.empty());
        lines.add(Line.styled("Press space to start", theme.accent()));
        return Text.from(lines);
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
            for (Game.Cart c : game.carts()) {
                if (c.y() == y && c.x() >= 0 && c.x() < Level.WIDTH) {
                    cells[c.x()] = theme.cart(game, c);
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
