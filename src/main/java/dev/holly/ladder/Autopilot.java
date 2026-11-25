package dev.holly.ladder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Finds a way through a level, for the attract-mode demo and to prove levels can be finished.
 *
 * <p>The search is breadth-first over the player's inputs, one per player step, and replays each candidate
 * from the start, so it uses the real game rules rather than a model of them. It plans with boulders off;
 * {@link #demo} then looks for a random seed under which the route also survives the boulders.
 */
final class Autopilot {

    enum Input { NONE, LEFT, RIGHT, UP, DOWN, JUMP }

    /** A route through a level: replay {@link #inputs} on a game started with {@link #seed}. */
    record Route(int levelNumber, long seed, List<Input> inputs) {}

    private record Node(Node parent, Input input, int depth) {}

    private static final int SEEDS_TO_TRY = 300;

    private Autopilot() {
    }

    /** The shortest list of inputs that finishes {@code level} with boulders off, or null if none does. */
    static List<Input> solve(Level level) {
        Level plan = level.withoutBoulders();
        int maxSteps = level.timeLimit() * Game.TICKS_PER_SECOND / Game.PLAYER_PERIOD;
        Set<String> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Node root = new Node(null, Input.NONE, 0);
        queue.add(root);
        seen.add(key(play(start(plan, 1), inputs(root))));
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.depth() >= maxSteps) {
                continue;
            }
            for (Input input : Input.values()) {
                Node next = new Node(node, input, node.depth() + 1);
                Game g = play(start(plan, 1), inputs(next));
                if (g.state() == Game.State.LEVEL_DONE) {
                    return inputs(next);
                }
                if (g.state() == Game.State.PLAYING && seen.add(key(g))) {
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /**
     * A route for the demo of level {@code number} (1 is the first), with boulders on: the boulder-free
     * route, and the first seed whose boulders (and trampolines) let it through. If no seed does, the
     * demo lad is in for a nasty surprise, as arcade demo players often are.
     */
    static Route demo(List<Level> levels, int number) {
        List<Input> inputs = solve(levels.get(number - 1));
        if (inputs == null) {
            return null;
        }
        for (long seed = 1; seed <= SEEDS_TO_TRY; seed++) {
            if (play(start(levels, number, seed), inputs).state() == Game.State.LEVEL_DONE) {
                return new Route(number, seed, inputs);
            }
        }
        return new Route(number, 1, inputs);
    }

    /** A new game on level {@code number} of {@code levels}, as the demo plays it. */
    static Game start(List<Level> levels, int number, long seed) {
        Game g = new Game(levels, new Random(seed));
        g.cheatToLevel(number);
        return g;
    }

    private static Game start(Level level, long seed) {
        return start(List.of(level), 1, seed);
    }

    /** Feeds one input, then runs the game for one player step. */
    static void step(Game g, Input input) {
        apply(g, input);
        for (int i = 0; i < Game.PLAYER_PERIOD; i++) {
            g.tick();
        }
    }

    /** Presses the key for one input, as a player would. */
    static void apply(Game g, Input input) {
        switch (input) {
            case LEFT -> g.left();
            case RIGHT -> g.right();
            case UP -> g.up();
            case DOWN -> g.down();
            case JUMP -> g.jump();
            case NONE -> { }
        }
    }

    private static Game play(Game g, List<Input> inputs) {
        for (Input input : inputs) {
            step(g, input);
            if (g.state() != Game.State.PLAYING) {
                break;
            }
        }
        return g;
    }

    private static List<Input> inputs(Node node) {
        List<Input> inputs = new ArrayList<>(node.depth());
        for (Node n = node; n.parent() != null; n = n.parent()) {
            inputs.add(n.input());
        }
        return inputs.reversed();
    }

    private static String key(Game g) {
        Game.Player p = g.player();
        return p.x + "," + p.y + "," + p.dir + "," + p.climbing + "," + p.wantVert + ","
                + p.wantJump + "," + p.jumpStep + "," + p.jumpDir;
    }
}
