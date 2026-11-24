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
            case TREASURE -> new Cell("&", GREEN);
            case EXIT -> new Cell("$", GREEN);
            case DISPENSER -> new Cell("V", GREEN);
            case PIT -> new Cell("*", GREEN);
            case SPIKES -> new Cell("^", GREEN);
            case WALL -> new Cell("|", GREEN);
            case TRAMPOLINE -> new Cell(".", GREEN);
            case EMPTY -> EMPTY;
        };
    }

    @Override
    public Cell boulder(Game game, Game.Boulder boulder) {
        return new Cell("o", GREEN);
    }

    @Override
    public Cell[] sign(String name) {
        String text = name;
        Cell[] cells = new Cell[text.length()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(String.valueOf(text.charAt(i)), GREEN);
        }
        return cells;
    }

    @Override
    public Cell chain() {
        return new Cell("|", GREEN);
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

    /** Lads (lives), level, score, time and best score, under the bottom of the map. */
    @Override
    public Line infoLine(Game game) {
        if (game.level() == null) {
            return Line.styled(" Best: " + game.highScore(), GREEN);
        }
        String name = game.level().hasSign() ? "" : "   " + game.level().name();
        return Line.styled(" Lads: " + Math.max(0, game.lives()) + "   Level: " + game.levelNumber()
                + "   Score: " + game.score() + "   Time: " + game.secondsLeft()
                + "   Best: " + game.highScore() + name, GREEN);
    }

    @Override
    public String helpText(Game game) {
        return switch (game.state()) {
            case TITLE -> " space: start   s or left/right: style   q: quit";
            case HOW_TO_PLAY -> " any key: start    q: quit";
            case GAME_OVER -> " space: start    q: quit";
            case PAUSED -> " p or space: resume    q: quit";
            default -> " arrows move  space jump  b stop  s style  p pause  q quit";
        };
    }

    @Override
    public String leftRightKeys() {
        return "<- ->";
    }

    @Override
    public String upDownKeys() {
        return "up dn";
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
                "Jump the rolling boulders. Grab the loot on the way.");
    }

    @Override
    public List<LegendEntry> legend() {
        return List.of(
                new LegendEntry(new Cell("p", GREEN), "you (q left, g still)"),
                new LegendEntry(new Cell("=", GREEN), "platform"),
                new LegendEntry(new Cell("H", GREEN), "ladder"),
                new LegendEntry(new Cell("o", GREEN), "boulder"),
                new LegendEntry(new Cell("&", GREEN), "treasure, " + Game.TREASURE_POINTS + " points"),
                new LegendEntry(new Cell("|", GREEN), "wall"),
                new LegendEntry(new Cell("*", GREEN), "star, swallows boulders"),
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
