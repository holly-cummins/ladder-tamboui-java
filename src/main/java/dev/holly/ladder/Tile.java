package dev.holly.ladder;

/** The static contents of one map cell. */
public enum Tile {
    EMPTY,
    ROCK,
    LADDER,
    TREASURE,
    EXIT,
    DISPENSER,
    PIT,
    SPIKES,
    WALL,
    TRAMPOLINE;

    static Tile fromMapChar(char c) {
        return switch (c) {
            case '#' -> ROCK;
            case 'H' -> LADDER;
            case '|' -> WALL;
            case '&' -> TREASURE;
            case 'E' -> EXIT;
            case 'v' -> DISPENSER;
            case '*' -> PIT;
            case '^' -> SPIKES;
            case '.' -> TRAMPOLINE;
            default -> EMPTY;
        };
    }
}
