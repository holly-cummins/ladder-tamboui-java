package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class GameTest {

    /** Builds a game from map rows; the rows are placed at the bottom of the 20-row map. */
    private static Game game(String... rows) {
        List<String> lines = new ArrayList<>(List.of("name: Test", "time: 60", "boulders: 0", "spawn: 1000", "---"));
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
                "  H  ",
                "##H##",
                "  H  ",
                "@ H  ",
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
                "  H    ",
                "##H####",
                "  H    ",
                "@ H    ",
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
    void treasureScoresAndTheExitEndsTheLevel() {
        Game g = game(
                "@ & E",
                "#####");
        g.right();
        ticks(g, playerSteps(4));
        assertEquals(Game.State.LEVEL_DONE, g.state());
        assertTrue(g.score() >= Game.TREASURE_POINTS + 50 * Game.TIME_BONUS_PER_SECOND);
    }

    @Test
    void jumpingOverSpikesIsSafe() {
        Game g = game(
                "@  ^     ",
                "#########");
        g.right();
        ticks(g, playerSteps(1));
        g.jump();
        ticks(g, playerSteps(8));
        assertEquals(Game.State.PLAYING, g.state());
        assertTrue(g.player().x() > 3);
    }

    @Test
    void jumpingWithUpPressedGrabsALadderInMidAir() {
        // A ladder hangs in the middle of a hole too wide to jump; its foot is out of reach from the floor.
        Game g = game(
                "         H     ",
                "#########H#####",
                "         H     ",
                "         H     ",
                "         H     ",
                "@        H     ",
                "#####    H   ##",
                "         H     ",
                "               ",
                "               ",
                "###############");
        int standing = Level.HEIGHT - 6;
        g.right();
        ticks(g, playerSteps(4)); // run to the edge of the hole
        assertEquals(4, g.player().x());
        g.jump();
        g.up();
        ticks(g, playerSteps(6));
        assertEquals(9, g.player().x(), "grabbed the ladder");
        assertTrue(g.player().y() < standing, "and climbed it");
        assertEquals(Game.State.PLAYING, g.state());
    }

    @Test
    void anUpPressMadeLongBeforeTheLadderStillCatchesIt() {
        Game g = game(
                "                         H",
                "#########################H",
                "                         H",
                "@                        H",
                "##########################");
        g.up(); // pressed once, far from any ladder
        g.right();
        ticks(g, playerSteps(40));
        assertEquals(25, g.player().x());
        assertTrue(g.player().y() < Level.HEIGHT - 2, "caught the ladder and climbed it");
    }

    @Test
    void walkingPastALadderWithoutPressingUpGoesStraightThrough() {
        Game g = game(
                "   H     ",
                "###H#####",
                "   H     ",
                "@  H    #",
                "#########");
        g.right();
        ticks(g, playerSteps(20));
        assertEquals(7, g.player().x());
        assertEquals(Level.HEIGHT - 2, g.player().y());
    }

    @Test
    void aDownPressWhileRunningCatchesTheNextLadderDown() {
        Game g = game(
                "@     ",
                "####H#",
                "    H ",
                "    H ",
                "######");
        g.down(); // no ladder here: doesn't stop you, just waits for one
        g.right();
        ticks(g, playerSteps(4));
        assertEquals(4, g.player().x(), "kept running to the ladder");
        ticks(g, playerSteps(6));
        assertEquals(Level.HEIGHT - 2, g.player().y(), "and climbed down it");
    }

    @Test
    void jumpsCanBeSteeredOrStoppedInMidAir() {
        Game g = game(
                "          @          ",
                "#####################");
        int start = g.player().x();
        g.right();
        g.jump();
        ticks(g, playerSteps(2));
        assertTrue(g.player().x() > start);
        g.left(); // turn round in mid-air
        ticks(g, playerSteps(8));
        assertTrue(g.player().x() < start, "came back the other way");

        Game h = game(
                "          @          ",
                "#####################");
        h.right();
        h.jump();
        ticks(h, playerSteps(2));
        int x = h.player().x();
        h.stop(); // drop straight down from here
        ticks(h, playerSteps(8));
        assertEquals(x, h.player().x());
        assertEquals(Level.HEIGHT - 2, h.player().y());
    }

    @Test
    void cheatStartsOnALaterLevelFromTheTitleOnly() {
        Game g = new Game(Level.loadAll(), new Random(1));
        g.cheatToLevel(99); // no such level
        assertEquals(Game.State.TITLE, g.state());
        g.cheatToLevel(2);
        assertEquals(Game.State.PLAYING, g.state());
        assertEquals(2, g.levelNumber());
        g.cheatToLevel(3); // not mid-game
        assertEquals(2, g.levelNumber());
    }

    @Test
    void howToPlayIsShownBeforeTheFirstGameOnly() {
        Game g = new Game(Level.loadAll(), new Random(1));
        assertEquals(Game.State.TITLE, g.state());
        g.confirm();
        assertEquals(Game.State.HOW_TO_PLAY, g.state());
        g.confirm();
        assertEquals(Game.State.PLAYING, g.state());
    }

    @Test
    void jumpingPastALadderWithoutPressingUpDoesNotGrabIt() {
        Game g = game(
                "@   H     ",
                "####H#####",
                "    H     ",
                "##########");
        g.right();
        ticks(g, playerSteps(1));
        g.jump();
        ticks(g, playerSteps(8));
        assertEquals(Level.HEIGHT - 4, g.player().y(), "still on the same floor");
        assertTrue(g.player().x() > 4);
    }

    @Test
    void stopHaltsRunningAndClimbing() {
        Game g = game(
                "     H",
                "     H",
                "@    H",
                "######");
        g.right();
        ticks(g, playerSteps(2));
        g.stop();
        ticks(g, playerSteps(5));
        assertEquals(2, g.player().x(), "stopped running");

        g.right();
        ticks(g, playerSteps(3));
        assertEquals(5, g.player().x());
        g.up();
        ticks(g, playerSteps(1));
        int y = g.player().y();
        g.stop();
        ticks(g, playerSteps(5));
        assertEquals(y, g.player().y(), "hangs on the ladder where it stopped");
        assertEquals(Game.State.PLAYING, g.state());
    }

    @Test
    void pipeWallsBlockWalking() {
        Game g = game(
                "@   |  ",
                "#######");
        g.right();
        ticks(g, playerSteps(10));
        assertEquals(3, g.player().x());
    }

    @Test
    void jumpsOverALowPipe() {
        Game g = game(
                "@   |     ",
                "##########");
        g.right();
        ticks(g, playerSteps(2));
        g.jump();
        ticks(g, playerSteps(8));
        assertTrue(g.player().x() > 4, "cleared the pipe");
        assertEquals(Level.HEIGHT - 2, g.player().y());
    }

    @Test
    void spikesKill() {
        Game g = game(
                "@ ^  ",
                "#####");
        g.right();
        ticks(g, playerSteps(2));
        assertEquals(Game.State.DYING, g.state());
    }

    @Test
    void landingOnATrampolineBouncesThePlayer() {
        Game g = game(
                "@        ",
                "##       ",
                "         ",
                "  .      ",
                "#########");
        int floor = Level.HEIGHT - 2;
        g.right();
        ticks(g, playerSteps(2)); // walk off the ledge
        int lowest = Integer.MIN_VALUE;
        boolean wentBackUp = false;
        for (int i = 0; i < 6; i++) {
            ticks(g, playerSteps(1));
            lowest = Math.max(lowest, g.player().y());
            if (lowest == floor && g.player().y() < floor) {
                wentBackUp = true;
            }
        }
        assertTrue(wentBackUp, "bounced back up after landing on the trampoline");
    }

    @Test
    void walkingOntoATrampolineDoesNotBounce() {
        Game g = game(
                "@ .   ",
                "######");
        g.right();
        ticks(g, playerSteps(3));
        assertEquals(Level.HEIGHT - 2, g.player().y());
        assertEquals(3, g.player().x());
    }

    @Test
    void bouldersKill() {
        List<String> lines = new ArrayList<>(List.of("boulders: 1", "spawn: 1", "---"));
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
        assertTrue(g.state() != Game.State.PLAYING, "a boulder bouncing along the only floor eventually hits a still player");
    }

    @Test
    void losingAllLivesEndsTheGame() {
        Game g = game(
                "@^",
                "##");
        for (int i = 0; i < Game.STARTING_LIVES; i++) {
            g.right();
            ticks(g, 100);
        }
        assertEquals(Game.State.GAME_OVER, g.state());
    }

    @Test
    void signMarkerIsParsedAndLeavesAnEmptyCell() {
        List<String> lines = new ArrayList<>(List.of("name: Test", "---", "", "  [", "", "@"));
        Level level = Level.parse(lines);
        assertTrue(level.hasSign());
        assertEquals(2, level.signX());
        assertEquals(1, level.signY());
        assertEquals(Tile.EMPTY, level.copyTiles()[1][2]);
    }

    @Test
    void shippedLevelsLoad() {
        List<Level> levels = Level.loadAll();
        assertTrue(!levels.isEmpty());
        for (Level level : levels) {
            assertTrue(!level.dispensers().isEmpty(), level.name() + " has a boulder dispenser");
            Tile[][] tiles = level.copyTiles();
            for (int[] d : level.dispensers()) {
                int x = d[0], y = d[1];
                boolean inTheAir = y + 1 < Level.HEIGHT && tiles[y + 1][x] == Tile.EMPTY;
                boolean pipeBeside = (x > 0 && tiles[y][x - 1] == Tile.WALL)
                        || (x + 1 < Level.WIDTH && tiles[y][x + 1] == Tile.WALL);
                assertTrue(!(inTheAir && pipeBeside),
                        level.name() + ": the dispenser at " + x + "," + y + " is in mid-air, so has no pipe beside it");
            }
        }
    }
}
