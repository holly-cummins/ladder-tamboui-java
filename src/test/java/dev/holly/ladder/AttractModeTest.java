package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AttractModeTest {

    @Test
    void demoStartsWhenIdleClearsTheLevelAndSaysQuicheHalfway() throws InterruptedException {
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
        int quicheTicks = 0;
        int ticks = 0;
        boolean quicheBeforeClear = false;
        while (attract.running() && ticks++ < 10_000) {
            attract.tick();
            if (attract.showQuiche()) {
                quicheTicks++;
                quicheBeforeClear |= !cleared;
            }
            cleared |= attract.running() && attract.game().state() == Game.State.LEVEL_DONE;
        }
        assertTrue(cleared, "the demo lad reached the treasure");
        assertTrue(quicheBeforeClear && quicheTicks > 0, "You eat quiche! showed partway through");
        assertFalse(attract.running(), "and the demo ended by itself");
    }
}
