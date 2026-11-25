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
 *   #  rock floor / wall        H  ladder
 *   |  pipe wall (solid)
 *   &  treasure                 E  exit lift
 *   v  boulder dispenser         *  star that swallows boulders
 *          (boulders roll away from a | pipe beside a dispenser on the floor;
 *           a dispenser in mid-air has no pipe and just drops them)
 *   ^  spikes (deadly)          .  trampoline
 *   @  player start             [  left end of the sign showing the level name
 *                                  (it hangs on chains from whatever is above)
 * </pre>
 * Ladders should run from the standing row of the lower floor, through the upper floor,
 * up to the standing row above it.
 */
public final class Level {

    public static final int WIDTH = 60;
    public static final int HEIGHT = 20;

    private final String name;
    private final int timeLimit;
    private final int maxBoulders;
    private final int spawnInterval;
    private final Tile[][] tiles;
    private final List<int[]> dispensers = new ArrayList<>();
    private final int startX;
    private final int startY;
    private final int signX;
    private final int signY;

    private Level(String name, int timeLimit, int maxBoulders, int spawnInterval, Tile[][] tiles, int startX, int startY,
            int signX, int signY) {
        this.name = name;
        this.timeLimit = timeLimit;
        this.maxBoulders = maxBoulders;
        this.spawnInterval = spawnInterval;
        this.tiles = tiles;
        this.startX = startX;
        this.startY = startY;
        this.signX = signX;
        this.signY = signY;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (tiles[y][x] == Tile.DISPENSER) {
                    dispensers.add(new int[] {x, y});
                }
            }
        }
    }

    /** The same level with no boulders, for planning a route around the rest of it. */
    Level withoutBoulders() {
        return new Level(name, timeLimit, 0, spawnInterval, tiles, startX, startY, signX, signY);
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
        int signX = -1;
        int signY = -1;
        for (int y = 0; y < HEIGHT; y++) {
            String row = i + y < lines.size() ? lines.get(i + y) : "";
            for (int x = 0; x < WIDTH; x++) {
                char c = x < row.length() ? row.charAt(x) : ' ';
                if (c == '[') {
                    signX = x;
                    signY = y;
                    c = ' ';
                }
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
                Integer.parseInt(header.getOrDefault("boulders", "4")),
                Integer.parseInt(header.getOrDefault("spawn", "60")),
                tiles, sx, sy, signX, signY);
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

    /** A fresh, mutable copy of the tile grid, so treasure can be collected without changing the level. */
    public Tile[][] copyTiles() {
        Tile[][] copy = new Tile[HEIGHT][];
        for (int y = 0; y < HEIGHT; y++) {
            copy[y] = tiles[y].clone();
        }
        return copy;
    }

    public String name() { return name; }
    /** True if the map places a sign showing the level name. */
    public boolean hasSign() { return signY >= 0; }
    /** Left end of the name sign, or -1 if there is none. */
    public int signX() { return signX; }
    public int signY() { return signY; }
    public int timeLimit() { return timeLimit; }
    public int maxBoulders() { return maxBoulders; }
    public int spawnInterval() { return spawnInterval; }
    public List<int[]> dispensers() { return dispensers; }
    public int startX() { return startX; }
    public int startY() { return startY; }
}
