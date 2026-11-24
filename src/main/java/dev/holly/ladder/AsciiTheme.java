package dev.holly.ladder;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderSet;
import dev.tamboui.widgets.block.Borders;

/**
 * Plain 7-bit ASCII in a single colour, green on black, like a classic green-screen terminal.
 *
 * <p>The lad is {@code p} or {@code q} depending on which way they face, or {@code g} when still.
 */
public final class AsciiTheme implements Theme {

    /** The only style: classic phosphor green on black, like a green-screen terminal. */
    private static final Style GREEN = Style.EMPTY.fg(Color.GREEN).bg(Color.BLACK);

    private static final BorderSet BOX = BorderSet.builder()
            .horizontal("-").vertical("|")
            .topLeft("+").topRight("+").bottomLeft("+").bottomRight("+")
            .build();
    private static final BorderSet DOUBLE_BOX = BorderSet.builder()
            .horizontal("=").vertical("|")
            .topLeft("+").topRight("+").bottomLeft("+").bottomRight("+")
            .build();

    private static final Cell EMPTY = new Cell(" ", GREEN);

    @Override
    public String name() {
        return "ascii";
    }

    @Override
    public Cell tile(Game game, int x, int y) {
        return switch (game.tile(x, y)) {
            case ROCK -> new Cell("=", GREEN);
            case LADDER -> new Cell("H", GREEN);
            case GEM -> new Cell("&", GREEN);
            case EXIT -> new Cell("$", GREEN);
            case CHUTE -> new Cell("V", GREEN);
            case PIT -> new Cell("*", GREEN);
            case SPIKES -> new Cell("^", GREEN);
            case TRAMPOLINE -> new Cell(".", GREEN);
            case EMPTY -> EMPTY;
        };
    }

    @Override
    public Cell cart(Game game, Game.Cart cart) {
        return new Cell("o", GREEN);
    }

    @Override
    public Cell player(Game game) {
        if (game.state() == Game.State.DYING) {
            return new Cell((game.ticks() / 4) % 2 == 0 ? "X" : "g", GREEN);
        }
        Game.Player p = game.player();
        String glyph = p.isClimbing() ? "g" : switch (p.facing()) {
            case 1 -> "p";
            case -1 -> "q";
            default -> "g";
        };
        return new Cell(glyph, GREEN);
    }

    @Override
    public Style screen() {
        return GREEN;
    }

    @Override
    public Block.Builder fieldFrame() {
        return Block.builder().borders(Borders.ALL).customBorderSet(BOX).borderStyle(GREEN).style(GREEN);
    }

    @Override
    public Block.Builder bannerFrame() {
        return Block.builder().borders(Borders.ALL).customBorderSet(DOUBLE_BOX).borderStyle(GREEN).style(GREEN);
    }

    @Override
    public Line statusLine(Game game) {
        if (game.level() == null) {
            return Line.from(Span.styled(" Best ", GREEN), Span.styled(String.valueOf(game.highScore()), GREEN));
        }
        return Line.from(
                Span.styled(" " + game.levelNumber() + ": ", GREEN), Span.styled(game.level().name(), GREEN),
                Span.styled("  Score ", GREEN), Span.styled(String.valueOf(game.score()), GREEN),
                Span.styled("  Lives ", GREEN), Span.styled(String.valueOf(Math.max(0, game.lives())), GREEN),
                Span.styled("  Time ", GREEN), Span.styled(String.valueOf(game.secondsLeft()), GREEN),
                Span.styled("  Best ", GREEN), Span.styled(String.valueOf(game.highScore()), GREEN));
    }

    @Override
    public String helpText(Game game) {
        return switch (game.state()) {
            case TITLE -> " space: start   left/right: style   q: quit";
            case GAME_OVER -> " space: start    q: quit";
            case PAUSED -> " p or space: resume    q: quit";
            default -> " arrows/WASD move  space jump  p pause  q quit";
        };
    }

    @Override
    public List<Line> titleArt(long ticks) {
        List<Line> lines = new ArrayList<>();
        for (int r = 0; r < Banner.HEIGHT; r++) {
            lines.add(Line.styled(Banner.row(r), GREEN));
        }
        return lines;
    }

    @Override
    public List<String> blurb() {
        return List.of(
                "Reach the treasure before time runs out.",
                "Jump the falling boulders. Grab the loot on the way.");
    }

    @Override
    public List<LegendEntry> legend() {
        return List.of(
                new LegendEntry(new Cell("p", GREEN), "you (q left, g still)"),
                new LegendEntry(new Cell("=", GREEN), "platform"),
                new LegendEntry(new Cell("H", GREEN), "ladder"),
                new LegendEntry(new Cell("o", GREEN), "boulder"),
                new LegendEntry(new Cell("&", GREEN), "treasure, " + Game.GEM_POINTS + " points"),
                new LegendEntry(new Cell("^", GREEN), "spikes, deadly"),
                new LegendEntry(new Cell(".", GREEN), "trampoline, bounces you"),
                new LegendEntry(new Cell("$", GREEN), "main treasure, next level"));
    }

    @Override
    public Style label() {
        return GREEN;
    }

    @Override
    public Style value() {
        return GREEN;
    }

    @Override
    public Style plain() {
        return GREEN;
    }

    @Override
    public Style accent() {
        return GREEN;
    }
}
