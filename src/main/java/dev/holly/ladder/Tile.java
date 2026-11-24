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
    GAS;

    static Tile fromMapChar(char c) {
        return switch (c) {
            case '#' -> ROCK;
            case '|' -> LADDER;
            case '*' -> GEM;
            case 'E' -> EXIT;
            case 'v' -> CHUTE;
            case 'u' -> PIT;
            case '~' -> GAS;
            default -> EMPTY;
        };
    }
}
