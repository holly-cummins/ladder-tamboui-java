package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import dev.tamboui.text.Line;
import dev.tamboui.text.Text;

/**
 * Text that doesn't fit is silently clipped at the edge of the screen, so check every screen, in every
 * style, fits the space it's drawn into.
 */
class ScreenFitTest {

    /** Inside the field's border. */
    private static final int INNER_W = Level.WIDTH;
    private static final int INNER_H = Level.HEIGHT;
    /** The info and help lines run the full width, border included. */
    private static final int OUTER_W = Level.WIDTH + 2;

    @Test
    void titleScreenFits() {
        for (String style : Theme.NAMES) {
            Renderer r = new Renderer(Theme.named(style));
            assertFits(style + " title", r.titleText(newGame()), INNER_W, INNER_H);
        }
    }

    @Test
    void howToPlayScreenFits() {
        for (String style : Theme.NAMES) {
            Renderer r = new Renderer(Theme.named(style));
            assertFits(style + " how to play", r.howToPlayText(), INNER_W, INNER_H);
        }
    }

    @Test
    void infoAndHelpLinesFitInEveryState() {
        for (String style : Theme.NAMES) {
            Theme theme = Theme.named(style);
            for (Game game : gamesInEveryState()) {
                String where = style + " " + game.state() + " level " + game.levelNumber();
                assertWidth(where + " info line", theme.infoLine(game), OUTER_W);
                assertWidth(where + " help line", Line.from(theme.helpText(game)), OUTER_W);
            }
        }
    }

    @Test
    void everySignFitsOnTheMap() {
        for (String style : Theme.NAMES) {
            Theme theme = Theme.named(style);
            for (Level level : Level.loadAll()) {
                if (level.hasSign()) {
                    int end = level.signX() + theme.sign(level.name()).length;
                    assertTrue(end <= Level.WIDTH,
                            style + ": the sign for " + level.name() + " runs " + (end - Level.WIDTH) + " off the map");
                }
            }
        }
    }

    private static Game newGame() {
        return new Game(Level.loadAll(), new Random(1));
    }

    private static List<Game> gamesInEveryState() {
        List<Game> games = new ArrayList<>();
        games.add(newGame()); // title
        Game howTo = newGame();
        howTo.confirm();
        games.add(howTo);
        for (int n = 1; n <= Level.loadAll().size(); n++) {
            Game playing = newGame();
            playing.cheatToLevel(n);
            games.add(playing);
        }
        Game paused = newGame();
        paused.cheatToLevel(1);
        paused.togglePause();
        games.add(paused);
        return games;
    }

    private static void assertFits(String where, Text text, int width, int height) {
        assertTrue(text.lines().size() <= height,
                where + " has " + text.lines().size() + " lines; only " + height + " fit");
        for (Line line : text.lines()) {
            assertWidth(where, line, width);
        }
    }

    private static void assertWidth(String where, Line line, int width) {
        assertTrue(line.width() <= width,
                where + ": this line is " + line.width() + " wide; only " + width + " fit: " + plain(line));
    }

    private static String plain(Line line) {
        StringBuilder sb = new StringBuilder();
        line.spans().forEach(s -> sb.append(s.content()));
        return "\"" + sb + "\"";
    }
}
