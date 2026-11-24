package dev.holly.ladder;

/**
 * The big "Ladder" title, in two bitmap fonts. Both have two-wide uprights and proper lowercase,
 * with ascenders on the L and d.
 * <ul>
 * <li>{@link #row}: 7 text rows, each letter drawn with its own character (Ls for the L, as for the a...).
 * <li>{@link #pixel}: 10 pixels high, meant to be drawn two pixels per cell with half blocks.
 * </ul>
 */
final class Banner {

    static final String WORD = "Ladder";

    static final int HEIGHT = 7;
    static final int GAP = 2;

    static final int PIXEL_HEIGHT = 10;
    static final int PIXEL_GAP = 2;

    private static final String[] TEXT_L = {
            "##    ",
            "##    ",
            "##    ",
            "##    ",
            "##    ",
            "##    ",
            "######"};
    private static final String[] TEXT_A = {
            "       ",
            "       ",
            " ####  ",
            "##  ## ",
            "##  ## ",
            "##  ## ",
            " ### ##"};
    private static final String[] TEXT_D = {
            "    ## ",
            "    ## ",
            " ##### ",
            "##  ## ",
            "##  ## ",
            "##  ## ",
            " ### ##"};
    private static final String[] TEXT_E = {
            "      ",
            "      ",
            " #### ",
            "##  ##",
            "######",
            "##    ",
            " #####"};
    private static final String[] TEXT_R = {
            "        ",
            "        ",
            "## #### ",
            "###   ##",
            "##      ",
            "##      ",
            "##      "};

    private static final String[] PIXEL_L = {
            "##....", "##....", "##....", "##....", "##....", "##....", "##....", "##....", "######", "######"};
    private static final String[] PIXEL_A = {
            "......", "......", "......", ".#####", "######", "##..##", "##..##", "##..##", "######", ".#####"};
    private static final String[] PIXEL_D = {
            "....##", "....##", "....##", ".#####", "######", "##..##", "##..##", "##..##", "######", ".#####"};
    private static final String[] PIXEL_E = {
            "......", "......", "......", ".####.", "######", "##..##", "######", "##....", "######", ".#####"};
    private static final String[] PIXEL_R = {
            "......", "......", "......", "##.###", "######", "###...", "##....", "##....", "##....", "##...."};

    private static final String[] ROWS = join(HEIGHT, GAP, false);
    private static final String[] PIXELS = join(PIXEL_HEIGHT, PIXEL_GAP, true);

    static final int WIDTH = ROWS[0].length();
    static final int PIXEL_WIDTH = PIXELS[0].length();

    private Banner() {
    }

    /** Lays the letters of {@link #WORD} side by side; text-font ink becomes the letter itself. */
    private static String[] join(int height, int gap, boolean pixels) {
        String[] rows = new String[height];
        for (int r = 0; r < height; r++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < WORD.length(); i++) {
                char letter = WORD.charAt(i);
                if (i > 0) {
                    sb.append(pixels ? ".".repeat(gap) : " ".repeat(gap));
                }
                String row = glyph(letter, pixels)[r];
                sb.append(pixels ? row : row.replace('#', letter));
            }
            rows[r] = sb.toString();
        }
        return rows;
    }

    private static String[] glyph(char c, boolean pixels) {
        return switch (c) {
            case 'L' -> pixels ? PIXEL_L : TEXT_L;
            case 'a' -> pixels ? PIXEL_A : TEXT_A;
            case 'd' -> pixels ? PIXEL_D : TEXT_D;
            case 'e' -> pixels ? PIXEL_E : TEXT_E;
            case 'r' -> pixels ? PIXEL_R : TEXT_R;
            default -> throw new IllegalArgumentException("No banner glyph for " + c);
        };
    }

    /** Row r of the text font, e.g. {@code "LL        dd ..."}. */
    static String row(int r) {
        return ROWS[r];
    }

    /** True if pixel (x, y) of the pixel font is set; anything outside the banner is not. */
    static boolean pixel(int x, int y) {
        return y >= 0 && y < PIXEL_HEIGHT && x >= 0 && x < PIXEL_WIDTH && PIXELS[y].charAt(x) == '#';
    }
}
