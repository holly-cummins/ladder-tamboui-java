package dev.holly.ladder;

import java.util.List;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.widgets.block.Block;

/**
 * How the game looks: glyphs, colours and borders. The {@link Renderer} handles layout and asks the theme
 * what to draw in each place.
 */
public interface Theme {

    /** One screen cell. */
    record Cell(String glyph, Style style) {}

    /** One row of the title-screen key. */
    record LegendEntry(Cell cell, String meaning) {}

    /** Command-line name, as in {@code --style=<name>}. */
    String name();

    /** The static map contents at (x, y). */
    Cell tile(Game game, int x, int y);

    Cell cart(Game game, Game.Cart cart);

    /** The level-name sign, one cell per column; its two ends hang from {@link #chain()}s. */
    Cell[] sign(String name);

    /** One link of the chains the sign hangs from. */
    Cell chain();

    Cell player(Game game);

    /** Fills the whole terminal behind the game. */
    default Style screen() {
        return Style.EMPTY;
    }

    /** A frame for the playing field; the renderer adds the title. */
    Block.Builder fieldFrame();

    /** A frame for pop-up banners such as PAUSED. */
    Block.Builder bannerFrame();

    /** Text along the bottom border of the field, or null for none. */
    default Line fieldFooter(Game game) {
        return null;
    }

    /** The line under the playing field: lives, level, score and so on. */
    Line infoLine(Game game);

    /** Banner shown when the clock runs out. */
    default String outOfTimeBanner() {
        return "OUT OF TIME";
    }

    String helpText(Game game);

    /** The big title for the splash screen; may animate with the game clock. */
    List<Line> titleArt(long ticks);

    List<String> blurb();

    List<LegendEntry> legend();

    Style label();

    Style value();

    Style plain();

    Style accent();

    /** Every style name, in the order the title-screen switcher shows them. */
    List<String> NAMES = List.of("ascii", "modern");

    static Theme named(String name) {
        return switch (name) {
            case "ascii" -> new AsciiTheme();
            case "modern" -> new ModernTheme();
            default -> throw new IllegalArgumentException("Unknown style '" + name + "'. Use ascii or modern.");
        };
    }
}
