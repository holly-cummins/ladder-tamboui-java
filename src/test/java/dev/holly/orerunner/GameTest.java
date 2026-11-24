package dev.holly.orerunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class GameTest {

    /** Builds a game from map rows; the rows are placed at the bottom of the 20-row map. */
    private static Game game(String... rows) {
        List<String> lines = new ArrayList<>(List.of("name: Test", "time: 60", "carts: 0", "spawn: 1000", "---"));
        for (int i = 0; i < Level.HEIGHT - rows.length; i++) {
            lines.add("");
        }
        lines.addAll(List.of(rows));
        Game g = new Game(List.of(Level.parse(lines)), new Random(1));
        g.newGame();
        return g;
    }

    private static void ticks(Game g, int n) {
        for (int i = 0; i < n; i++) {
            g.tick();
        }
    }

    private static int playerSteps(int steps) {
        return steps * Game.PLAYER_PERIOD;
    }

    @Test
    void runningContinuesUntilAWall() {
        Game g = game(
                "@        #",
                "##########");
        g.right();
        ticks(g, playerSteps(20));
        assertEquals(8, g.player().x());
        assertEquals(0, g.player().facing(), "stops at the wall");
    }

    @Test
    void climbsALadderToTheTop() {
        Game g = game(
                "  |  ",
                "##|##",
                "  |  ",
                "@ |  ",
                "#####");
        int bottom = Level.HEIGHT - 2;
        g.right();
        ticks(g, playerSteps(1));
        g.up(); // requested early: grabbed when the player reaches the ladder
        ticks(g, playerSteps(10));
        assertEquals(2, g.player().x());
        assertEquals(bottom - 3, g.player().y());
        assertTrue(!g.player().isClimbing());
    }

    @Test
    void stepsOffTheLadderSidewaysAtFloorLevel() {
        Game g = game(
                "  |    ",
                "##|####",
                "  |    ",
                "@ |    ",
                "#######");
        g.right();
        ticks(g, playerSteps(2));
        g.up();
        ticks(g, playerSteps(1));
        assertTrue(g.player().isClimbing());
        g.right(); // pressed mid-climb: leave the ladder once there is floor beside it
        ticks(g, playerSteps(6));
        assertEquals(Level.HEIGHT - 5, g.player().y());
        assertTrue(g.player().x() > 2);
    }

    @Test
    void jumpsAThreeCellGap() {
        Game g = game(
                "@          ",
                "###   #####");
        g.right();
        ticks(g, playerSteps(2));
        g.jump();
        ticks(g, playerSteps(8));
        assertEquals(Level.HEIGHT - 2, g.player().y(), "landed on the far side, not in the gap");
        assertTrue(g.player().x() >= 6);
    }

    @Test
    void walkingIntoAGapFalls() {
        Game g = game(
                "@     ",
                "##  ##",
                "      ",
                "######");
        g.right();
        ticks(g, playerSteps(6));
        assertEquals(Level.HEIGHT - 2, g.player().y());
    }

    @Test
    void gemsScoreAndTheExitEndsTheLevel() {
        Game g = game(
                "@ * E",
                "#####");
        g.right();
        ticks(g, playerSteps(4));
        assertEquals(Game.State.LEVEL_DONE, g.state());
        assertTrue(g.score() >= Game.GEM_POINTS + 50 * Game.TIME_BONUS_PER_SECOND);
    }

    @Test
    void gasKills() {
        Game g = game(
                "@ ~  ",
                "#####");
        g.right();
        ticks(g, playerSteps(2));
        assertEquals(Game.State.DYING, g.state());
    }

    @Test
    void jumpingOverGasIsSafe() {
        Game g = game(
                "@  ~     ",
                "#########");
        g.right();
        ticks(g, playerSteps(1));
        g.jump();
        ticks(g, playerSteps(8));
        assertEquals(Game.State.PLAYING, g.state());
        assertTrue(g.player().x() > 3);
    }

    @Test
    void cartsKill() {
        List<String> lines = new ArrayList<>(List.of("carts: 1", "spawn: 1", "---"));
        for (int i = 0; i < Level.HEIGHT - 2; i++) {
            lines.add("");
        }
        lines.add("v    @");
        lines.add("######");
        Game g = new Game(List.of(Level.parse(lines)), new Random(1));
        g.newGame();
        for (int i = 0; i < 200 && g.state() == Game.State.PLAYING; i++) {
            g.tick();
        }
        assertTrue(g.state() != Game.State.PLAYING, "a cart bouncing along the only floor eventually hits a still player");
    }

    @Test
    void losingAllLivesEndsTheGame() {
        Game g = game(
                "@~",
                "##");
        for (int i = 0; i < Game.STARTING_LIVES; i++) {
            g.right();
            ticks(g, 100);
        }
        assertEquals(Game.State.GAME_OVER, g.state());
    }

    @Test
    void shippedLevelsLoad() {
        List<Level> levels = Level.loadAll();
        assertTrue(!levels.isEmpty());
        for (Level level : levels) {
            assertTrue(!level.chutes().isEmpty(), level.name() + " has a cart chute");
        }
    }
}
