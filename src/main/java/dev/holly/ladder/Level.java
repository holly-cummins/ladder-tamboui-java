package dev.holly.ladder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A level map, loaded from a plain-text file.
 * <p>
 * The file starts with {@code key: value} header lines, then a {@code ---} line, then the map.
 * Map characters (the editing alphabet, not what is drawn on screen):
 * <pre>
 *   #  rock floor / wall        |  ladder
 *   *  gem                      E  exit lift
 *   v  cart chute (spawner)     u  pit that swallows carts
 *   ~  gas vent (deadly)        @  player start
 * </pre>
 * Ladders should run from the standing row of the lower floor, through the upper floor,
 * up to the standing row above it.
 */
public final class Level {

    public static final int WIDTH = 60;
    public static final int HEIGHT = 20;

    private final String name;
    private final int timeLimit;
    private final int maxCarts;
    private final int spawnInterval;
    private final Tile[][] tiles;
    private final List<int[]> chutes = new ArrayList<>();
    private final int startX;
    private final int startY;

    private Level(String name, int timeLimit, int maxCarts, int spawnInterval, Tile[][] tiles, int startX, int startY) {
        this.name = name;
        this.timeLimit = timeLimit;
        this.maxCarts = maxCarts;
        this.spawnInterval = spawnInterval;
        this.tiles = tiles;
        this.startX = startX;
        this.startY = startY;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (tiles[y][x] == Tile.CHUTE) {
                    chutes.add(new int[] {x, y});
                }
            }
        }
    }

    /** Loads every level listed in {@code levels/index.txt} on the classpath. */
    public static List<Level> loadAll() {
        List<Level> levels = new ArrayList<>();
        for (String file : readLines("levels/index.txt")) {
            if (!file.isBlank()) {
                levels.add(parse(readLines("levels/" + file.strip())));
            }
        }
        if (levels.isEmpty()) {
            throw new IllegalStateException("No levels found in levels/index.txt");
        }
        return levels;
    }

    static Level parse(List<String> lines) {
        Map<String, String> header = new HashMap<>();
        int i = 0;
        for (; i < lines.size() && !lines.get(i).strip().equals("---"); i++) {
            String line = lines.get(i);
            int colon = line.indexOf(':');
            if (colon > 0) {
                header.put(line.substring(0, colon).strip(), line.substring(colon + 1).strip());
            }
        }
        i++; // skip ---

        Tile[][] tiles = new Tile[HEIGHT][WIDTH];
        int sx = 1;
        int sy = HEIGHT - 2;
        for (int y = 0; y < HEIGHT; y++) {
            String row = i + y < lines.size() ? lines.get(i + y) : "";
            for (int x = 0; x < WIDTH; x++) {
                char c = x < row.length() ? row.charAt(x) : ' ';
                if (c == '@') {
                    sx = x;
                    sy = y;
                    c = ' ';
                }
                tiles[y][x] = Tile.fromMapChar(c);
            }
        }
        return new Level(
                header.getOrDefault("name", "Unnamed"),
                Integer.parseInt(header.getOrDefault("time", "90")),
                Integer.parseInt(header.getOrDefault("carts", "4")),
                Integer.parseInt(header.getOrDefault("spawn", "60")),
                tiles, sx, sy);
    }

    private static List<String> readLines(String resource) {
        InputStream in = Level.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Missing resource " + resource);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A fresh, mutable copy of the tile grid, so gems can be collected without changing the level. */
    public Tile[][] copyTiles() {
        Tile[][] copy = new Tile[HEIGHT][];
        for (int y = 0; y < HEIGHT; y++) {
            copy[y] = tiles[y].clone();
        }
        return copy;
    }

    public String name() { return name; }
    public int timeLimit() { return timeLimit; }
    public int maxCarts() { return maxCarts; }
    public int spawnInterval() { return spawnInterval; }
    public List<int[]> chutes() { return chutes; }
    public int startX() { return startX; }
    public int startY() { return startY; }
}
