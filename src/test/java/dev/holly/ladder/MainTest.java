package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void finiteGamesAreModernUnlessAskedOtherwise() {
        assertEquals("ascii", Main.parseArgs(new String[0]).theme().name());
        Main.Options finite = Main.parseArgs(new String[]{"--finite"});
        assertTrue(finite.finite());
        assertEquals("modern", finite.theme().name());
        assertEquals("ascii", Main.parseArgs(new String[]{"--finite", "--style=ascii"}).theme().name());
    }
}
