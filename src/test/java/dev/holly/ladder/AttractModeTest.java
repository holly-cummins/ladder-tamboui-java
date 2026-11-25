package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AttractModeTest {

    @Test
    void theTitleScreenSaysQuicheHalfwayToTheDemo() {
        AttractMode attract = new AttractMode(Level.loadAll());
        int firstQuiche = -1;
        int quicheTicks = 0;
        for (int t = 0; t < AttractMode.IDLE_TICKS - 1; t++) {
            attract.idleTick();
            if (attract.showQuiche()) {
                quicheTicks++;
                if (firstQuiche < 0) {
                    firstQuiche = t + 1;
                }
            }
        }
        assertEquals(AttractMode.IDLE_TICKS / 2, firstQuiche, "You eat quiche! appears halfway");
        assertEquals(AttractMode.QUICHE_TICKS, quicheTicks, "for a few seconds");
        attract.resetIdle();
        assertFalse(attract.showQuiche(), "a key press clears it");
    }

    @Test
    void demoStartsWhenIdleAndClearsTheLevel() throws InterruptedException {
        List<Level> levels = Level.loadAll();
        AttractMode attract = new AttractMode(levels);
        long deadline = System.currentTimeMillis() + 20_000;
        while (!attract.running() && System.currentTimeMillis() < deadline) {
            for (int i = 0; i < AttractMode.IDLE_TICKS && !attract.running(); i++) {
                attract.idleTick();
            }
            if (!attract.running()) {
                Thread.sleep(50); // the first route is still being planned
            }
        }
        assertTrue(attract.running(), "a demo started");

        boolean cleared = false;
        int ticks = 0;
        while (attract.running() && ticks++ < 10_000) {
            attract.tick();
            assertFalse(attract.showQuiche(), "no quiche in the demo itself");
            cleared |= attract.running() && attract.game().state() == Game.State.LEVEL_DONE;
        }
        assertTrue(cleared, "the demo lad reached the treasure");
        assertFalse(attract.running(), "and the demo ended by itself");
    }
}
