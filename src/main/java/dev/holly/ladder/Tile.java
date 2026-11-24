package dev.holly.ladder;

/** The static contents of one map cell. */
public enum Tile {
    EMPTY,
    ROCK,
    LADDER,
    GEM,
    EXIT,
    CHUTE,
    PIT,
    SPIKES,
    WALL,
    TRAMPOLINE;

    static Tile fromMapChar(char c) {
        return switch (c) {
            case '#' -> ROCK;
            case 'H' -> LADDER;
            case '|' -> WALL;
            case '&' -> GEM;
            case 'E' -> EXIT;
            case 'v' -> CHUTE;
            case '*' -> PIT;
            case '^' -> SPIKES;
            case '.' -> TRAMPOLINE;
            default -> EMPTY;
        };
    }
}
