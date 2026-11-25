package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Proves every shipped level can be finished in time, using the {@link Autopilot}'s search. */
class LevelSolverTest {

    @Test
    void everyShippedLevelCanBeFinished() {
        for (Level level : Level.loadAll()) {
            assertNotNull(Autopilot.solve(level), level.name() + " cannot be finished");
        }
    }

    @Test
    void demoRoutesFinishTheirLevelWithBouldersOn() {
        List<Level> levels = Level.loadAll();
        for (int n = 1; n <= levels.size(); n++) {
            long start = System.nanoTime();
            Autopilot.Route route = Autopilot.demo(levels, n);
            Game g = Autopilot.start(levels, n, route.seed());
            for (Autopilot.Input input : route.inputs()) {
                Autopilot.step(g, input);
            }
            System.out.printf("demo %d: seed %d, %d steps, planned in %d ms, ends %s%n", n, route.seed(),
                    route.inputs().size(), (System.nanoTime() - start) / 1_000_000, g.state());
            assertEquals(Game.State.LEVEL_DONE, g.state(), levels.get(n - 1).name());
        }
    }
}
