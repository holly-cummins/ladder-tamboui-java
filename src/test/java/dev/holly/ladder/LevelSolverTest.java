package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Proves every shipped level can be finished: a breadth-first search over the player's inputs, one input
 * per player step, with boulders switched off. Nodes are replayed from the start, so the search uses the real
 * game rules rather than a model of them.
 */
class LevelSolverTest {

    private enum Input { NONE, LEFT, RIGHT, UP, DOWN, JUMP }

    private record Node(Node parent, Input input, int depth) {}

    @Test
    void everyShippedLevelCanBeFinished() throws IOException {
        for (String file : lines("levels/index.txt")) {
            if (file.isBlank()) {
                continue;
            }
            List<String> level = new ArrayList<>();
            for (String line : lines("levels/" + file.strip())) {
                level.add(line.startsWith("boulders:") ? "boulders: 0" : line);
            }
            Level parsed = Level.parse(level);
            assertTrue(solvable(parsed), parsed.name() + " (" + file.strip() + ") cannot be finished");
        }
    }

    private static boolean solvable(Level level) {
        int maxSteps = level.timeLimit() * Game.TICKS_PER_SECOND / Game.PLAYER_PERIOD;
        Map<String, Boolean> seen = new HashMap<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Node root = new Node(null, Input.NONE, 0);
        queue.add(root);
        seen.put(key(replay(level, root)), true);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.depth() >= maxSteps) {
                continue;
            }
            for (Input input : Input.values()) {
                Node next = new Node(node, input, node.depth() + 1);
                Game g = replay(level, next);
                if (g.state() == Game.State.LEVEL_DONE) {
                    return true;
                }
                if (g.state() != Game.State.PLAYING) {
                    continue;
                }
                if (seen.putIfAbsent(key(g), true) == null) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private static Game replay(Level level, Node node) {
        List<Input> inputs = new ArrayList<>();
        for (Node n = node; n.parent() != null; n = n.parent()) {
            inputs.add(0, n.input());
        }
        Game g = new Game(List.of(level), new Random(1));
        g.newGame();
        for (Input input : inputs) {
            switch (input) {
                case LEFT -> g.left();
                case RIGHT -> g.right();
                case UP -> g.up();
                case DOWN -> g.down();
                case JUMP -> g.jump();
                case NONE -> { }
            }
            for (int i = 0; i < Game.PLAYER_PERIOD; i++) {
                g.tick();
            }
            if (g.state() != Game.State.PLAYING) {
                break;
            }
        }
        return g;
    }

    private static String key(Game g) {
        Game.Player p = g.player();
        return p.x + "," + p.y + "," + p.dir + "," + p.climbing + "," + p.wantVert + "," + p.wantVertSteps + ","
                + p.wantJump + "," + p.jumpStep + "," + p.jumpDir;
    }

    private static List<String> lines(String resource) throws IOException {
        InputStream in = LevelSolverTest.class.getClassLoader().getResourceAsStream(resource);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().toList();
        }
    }
}
