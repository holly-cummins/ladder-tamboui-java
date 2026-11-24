package dev.holly.ladder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * All game state and rules. Advanced one fixed tick at a time by {@link #tick()}; knows nothing
 * about the terminal.
 */
public final class Game {

    public enum State { TITLE, PLAYING, PAUSED, DYING, LEVEL_DONE, GAME_OVER }

    /** Ticks per second of the game clock; {@link Main} drives {@link #tick()} at this rate. */
    public static final int TICKS_PER_SECOND = 20;

    static final int PLAYER_PERIOD = 2;          // player moves every N ticks
    static final int STARTING_LIVES = 4;
    static final int CLIMB_REQUEST_STEPS = 12;   // how long an up/down request waits for a ladder
    static final int[] JUMP_ARC = {-1, -1, 0, 1, 1};
    static final int TREASURE_POINTS = 100;
    static final int TIME_BONUS_PER_SECOND = 10;

    private static final int DYING_TICKS = 30;
    private static final int LEVEL_DONE_TICKS = 40;

    private final List<Level> levels;
    private final Random random;

    private State state = State.TITLE;
    private int levelIndex;
    private int loop;                // how many times every level has been completed
    private Level level;
    private Tile[][] tiles;
    private final Player player = new Player();
    private final List<Boulder> boulders = new ArrayList<>();

    private long ticks;
    private int stateTicks;
    private int timeLeftTicks;
    private int nextSpawnTick;
    private int score;
    private int lives;
    private int highScore;

    public Game(List<Level> levels, Random random) {
        this.levels = levels;
        this.random = random;
    }

    // ---------------------------------------------------------------- input

    public void left() { if (state == State.PLAYING) { player.dir = -1; } }
    public void right() { if (state == State.PLAYING) { player.dir = 1; } }

    public void up() {
        if (state == State.PLAYING) {
            player.wantVert = -1;
            player.wantVertSteps = CLIMB_REQUEST_STEPS;
        }
    }

    /** Climbs down if there is a ladder; otherwise stops, and grabs the next ladder down. */
    public void down() {
        if (state != State.PLAYING) {
            return;
        }
        if (player.climbing == 0 && !isLadder(player.x, player.y + 1)) {
            player.dir = 0;
        }
        player.wantVert = 1;
        player.wantVertSteps = CLIMB_REQUEST_STEPS;
    }

    /** Stops horizontal movement (used when the terminal reports key releases). */
    public void stopHorizontal(int dir) {
        if (state == State.PLAYING && player.dir == dir) {
            player.dir = 0;
        }
    }

    public void jump() {
        if (state == State.PLAYING) {
            player.wantJump = true;
        }
    }

    /** Space/Enter on non-playing screens. */
    public void confirm() {
        switch (state) {
            case TITLE, GAME_OVER -> newGame();
            case PAUSED -> state = State.PLAYING;
            case PLAYING -> jump();
            default -> { }
        }
    }

    public void togglePause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = State.PLAYING;
        }
    }

    // ---------------------------------------------------------------- lifecycle

    void newGame() {
        score = 0;
        lives = STARTING_LIVES;
        loop = 0;
        startLevel(0);
    }

    void startLevel(int index) {
        levelIndex = index;
        level = levels.get(index);
        tiles = level.copyTiles();
        respawn();
    }

    private void respawn() {
        player.reset(level.startX(), level.startY());
        boulders.clear();
        timeLeftTicks = level.timeLimit() * TICKS_PER_SECOND;
        nextSpawnTick = 0;
        stateTicks = 0;
        state = State.PLAYING;
    }

    // ---------------------------------------------------------------- simulation

    public void tick() {
        ticks++;
        stateTicks++;
        switch (state) {
            case PLAYING -> playTick();
            case DYING -> {
                if (stateTicks >= DYING_TICKS) {
                    lives--;
                    if (lives <= 0) {
                        highScore = Math.max(highScore, score);
                        state = State.GAME_OVER;
                        stateTicks = 0;
                    } else {
                        respawn();
                    }
                }
            }
            case LEVEL_DONE -> {
                if (stateTicks >= LEVEL_DONE_TICKS) {
                    int next = levelIndex + 1;
                    if (next >= levels.size()) {
                        next = 0;
                        loop++;
                    }
                    startLevel(next);
                }
            }
            default -> { }
        }
    }

    private void playTick() {
        if (--timeLeftTicks <= 0) {
            die();
            return;
        }
        spawnBoulders();
        if (stateTicks % PLAYER_PERIOD == 0) {
            int yBefore = player.y;
            stepPlayer();
            if (player.y > yBefore) {
                bounceOffTrampoline();
            }
            if (checkPlayerCell() || hitByBoulder()) {
                return;
            }
        }
        if (stateTicks % boulderPeriod() == 0) {
            stepBoulders();
            hitByBoulder();
        }
    }

    int boulderPeriod() {
        return Math.max(1, 3 - loop);
    }

    private void spawnBoulders() {
        if (stateTicks < nextSpawnTick || boulders.size() >= level.maxBoulders() || level.chutes().isEmpty()) {
            return;
        }
        int[] chute = level.chutes().get(random.nextInt(level.chutes().size()));
        boulders.add(new Boulder(chute[0], chute[1], launchDirection(chute[0], chute[1])));
        int interval = level.spawnInterval();
        nextSpawnTick = stateTicks + interval / 2 + random.nextInt(Math.max(1, interval));
    }

    private void stepPlayer() {
        Player p = player;
        if (p.wantVertSteps > 0 && --p.wantVertSteps == 0) {
            p.wantVert = 0;
        }

        if (p.jumpStep >= 0) {
            if (!grabLadderInMidAir()) {
                stepJump();
            }
            return;
        }

        boolean supported = isSupported(p.x, p.y);
        if (!supported && p.climbing == 0) {
            if (!grabLadderInMidAir()) {
                p.y++; // fall
            }
            return;
        }

        if (p.wantJump && p.climbing != 0) {
            p.wantJump = false; // no jumping off the middle of a ladder
        }
        if (p.wantJump) {
            p.wantJump = false;
            p.climbing = 0; // keep any up/down request: it grabs a ladder met in mid-air
            p.jumpDir = p.dir;
            p.jumpStep = 0;
            stepJump();
            return;
        }

        // Grab a ladder if one was requested and is here.
        if (p.wantVert == -1 && isLadder(p.x, p.y) && isLadder(p.x, p.y - 1)) {
            p.climbing = -1;
            p.wantVert = 0;
            p.dir = 0;
        } else if (p.wantVert == 1 && isLadder(p.x, p.y + 1)) {
            p.climbing = 1;
            p.wantVert = 0;
            p.dir = 0;
        }

        if (p.climbing != 0) {
            // Step off sideways once there is floor to step onto.
            if (p.dir != 0 && canEnter(p.x + p.dir, p.y) && isSupported(p.x + p.dir, p.y)) {
                p.climbing = 0;
                p.x += p.dir;
                return;
            }
            int ny = p.y + p.climbing;
            boolean canClimb = p.climbing < 0 ? isLadder(p.x, ny) : isLadder(p.x, p.y + 1);
            if (canClimb) {
                p.y = ny;
            }
            boolean more = p.climbing < 0 ? isLadder(p.x, p.y - 1) : isLadder(p.x, p.y + 1);
            if (!more) {
                p.climbing = 0; // any direction pressed during the climb carries on from here
            }
            return;
        }

        if (p.dir != 0) {
            if (canEnter(p.x + p.dir, p.y)) {
                p.x += p.dir;
            } else {
                p.dir = 0;
            }
        }
    }

    /**
     * Jumping or falling past a ladder grabs it if up or down was pressed recently, so jumping onto a
     * ladder is a way to reach ones that don't come down to the floor. The lad can reach one cell ahead,
     * so the jump doesn't have to pass exactly through the ladder.
     */
    private boolean grabLadderInMidAir() {
        Player p = player;
        if (p.wantVert == 0) {
            return false;
        }
        int ahead = p.jumpStep >= 0 ? p.jumpDir : p.dir;
        if (!isLadder(p.x, p.y)) {
            if (ahead == 0 || !isLadder(p.x + ahead, p.y) || !canEnter(p.x + ahead, p.y)) {
                return false;
            }
            p.x += ahead;
        }
        p.jumpStep = -1;
        p.jumpDir = 0;
        p.dir = 0;
        p.climbing = p.wantVert;
        p.wantVert = 0;
        return true;
    }

    private void stepJump() {
        Player p = player;
        int dy = JUMP_ARC[p.jumpStep];
        if (p.jumpDir != 0) {
            if (canEnter(p.x + p.jumpDir, p.y)) {
                p.x += p.jumpDir;
            } else {
                p.jumpDir = 0;
            }
        }
        if (dy < 0) {
            if (canEnter(p.x, p.y - 1)) {
                p.y--;
            } else {
                p.jumpStep = 2; // bumped head: start coming down
            }
        } else if (dy > 0) {
            if (isSupported(p.x, p.y)) {
                p.jumpStep = -1; // landed early
                return;
            }
            p.y++;
        }
        p.jumpStep++;
        if (p.jumpStep >= JUMP_ARC.length) {
            p.jumpStep = -1;
        }
    }

    /** Landing on a trampoline launches the player into a jump in a random direction. */
    private void bounceOffTrampoline() {
        Player p = player;
        if (tileAt(p.x, p.y) != Tile.TRAMPOLINE || !isSupported(p.x, p.y)) {
            return;
        }
        p.climbing = 0;
        p.wantVert = 0;
        p.wantJump = false;
        p.jumpDir = random.nextInt(3) - 1;
        p.dir = p.jumpDir;
        p.jumpStep = 0;
    }

    /** Boulders leave a chute away from the pipe beside it; with no pipe, either way. */
    int launchDirection(int x, int y) {
        boolean pipeLeft = tileAt(x - 1, y) == Tile.WALL;
        boolean pipeRight = tileAt(x + 1, y) == Tile.WALL;
        if (pipeLeft != pipeRight) {
            return pipeLeft ? 1 : -1;
        }
        return random.nextBoolean() ? 1 : -1;
    }

    /** Boulders only obey gravity and momentum: they fall, keep rolling the same way, and bounce off walls. */
    private void stepBoulders() {
        for (Iterator<Boulder> it = boulders.iterator(); it.hasNext(); ) {
            Boulder c = it.next();
            if (!isSupported(c.x, c.y)) {
                c.y++;
            } else if (canEnter(c.x + c.dir, c.y)) {
                c.x += c.dir;
            } else {
                c.dir = -c.dir;
            }
            if (c.y >= Level.HEIGHT || tileAt(c.x, c.y) == Tile.PIT) {
                it.remove();
            }
        }
    }

    /** Handles whatever the player is standing in. Returns true if the state changed. */
    private boolean checkPlayerCell() {
        Tile t = tileAt(player.x, player.y);
        switch (t) {
            case TREASURE -> {
                tiles[player.y][player.x] = Tile.EMPTY;
                score += TREASURE_POINTS;
            }
            case SPIKES -> {
                die();
                return true;
            }
            case EXIT -> {
                score += (timeLeftTicks / TICKS_PER_SECOND) * TIME_BONUS_PER_SECOND;
                highScore = Math.max(highScore, score);
                state = State.LEVEL_DONE;
                stateTicks = 0;
                return true;
            }
            default -> { }
        }
        if (player.y >= Level.HEIGHT - 1 && !isSupported(player.x, player.y)) {
            die(); // fell out of the map
            return true;
        }
        return false;
    }

    private boolean hitByBoulder() {
        if (state != State.PLAYING) {
            return false;
        }
        for (Boulder c : boulders) {
            if (c.x == player.x && c.y == player.y) {
                die();
                return true;
            }
        }
        return false;
    }

    private void die() {
        state = State.DYING;
        stateTicks = 0;
    }

    // ---------------------------------------------------------------- map queries

    Tile tileAt(int x, int y) {
        if (x < 0 || x >= Level.WIDTH || y < 0 || y >= Level.HEIGHT) {
            return Tile.ROCK; // the edges of the map are solid
        }
        return tiles[y][x];
    }

    boolean isLadder(int x, int y) {
        return tileAt(x, y) == Tile.LADDER;
    }

    /** Rock and pipe walls block movement. */
    boolean isSolid(int x, int y) {
        Tile t = tileAt(x, y);
        return t == Tile.ROCK || t == Tile.WALL;
    }

    boolean canEnter(int x, int y) {
        return !isSolid(x, y);
    }

    /** True if something at (x, y) would stand rather than fall. */
    boolean isSupported(int x, int y) {
        if (y + 1 >= Level.HEIGHT) {
            return false;
        }
        Tile below = tileAt(x, y + 1);
        return below == Tile.ROCK || below == Tile.WALL || below == Tile.LADDER;
    }

    // ---------------------------------------------------------------- accessors for rendering and tests

    public State state() { return state; }
    public Level level() { return level; }
    public int levelNumber() { return loop * levels.size() + levelIndex + 1; }
    public Tile tile(int x, int y) { return tileAt(x, y); }
    public Player player() { return player; }
    public List<Boulder> boulders() { return boulders; }
    public int score() { return score; }
    public int lives() { return lives; }
    public int highScore() { return highScore; }
    public int secondsLeft() { return (timeLeftTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND; }
    public long ticks() { return ticks; }

    // ---------------------------------------------------------------- entities

    /** The player. Fields are package-private so the rules above stay readable. */
    public static final class Player {
        int x;
        int y;
        int dir;            // -1 left, 0 still, 1 right
        int climbing;       // -1 up, 0 not climbing, 1 down
        int wantVert;       // pending ladder request
        int wantVertSteps;
        boolean wantJump;
        int jumpStep = -1;  // index into JUMP_ARC, or -1 when not jumping
        int jumpDir;

        void reset(int startX, int startY) {
            x = startX;
            y = startY;
            dir = 0;
            climbing = 0;
            wantVert = 0;
            wantVertSteps = 0;
            wantJump = false;
            jumpStep = -1;
            jumpDir = 0;
        }

        public int x() { return x; }
        public int y() { return y; }
        public int facing() { return jumpStep >= 0 ? jumpDir : dir; }
        public boolean isClimbing() { return climbing != 0; }
    }

    /** A boulder rolling out of a chute. */
    public static final class Boulder {
        int x;
        int y;
        int dir;

        Boulder(int x, int y, int dir) {
            this.x = x;
            this.y = y;
            this.dir = dir;
        }

        public int x() { return x; }
        public int y() { return y; }
    }
}
