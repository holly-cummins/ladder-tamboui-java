package dev.holly.ladder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The demo that plays itself when the title screen is left alone, as arcade machines did to attract
 * passers-by. Each demo plays one level, in turn, along a route the {@link Autopilot} found.
 */
final class AttractMode {

    static final int IDLE_TICKS = 10 * Game.TICKS_PER_SECOND;   // title screen time before a demo starts
    static final int QUICHE_TICKS = 3 * Game.TICKS_PER_SECOND;  // how long the halfway message shows
    private static final int END_TICKS = 3 * Game.TICKS_PER_SECOND / 2; // lingering on the clear or the crash

    private final List<Level> levels;
    private final Map<Integer, Autopilot.Route> routes = new ConcurrentHashMap<>();

    private int idleTicks;
    private int nextLevel = 1;

    private Game demo;
    private List<Autopilot.Input> inputs;
    private int step;
    private int ticks;
    private int quicheUntil = -1;
    private int endTicks;

    AttractMode(List<Level> levels) {
        this.levels = levels;
        Thread planner = new Thread(this::planAll, "demo-planner");
        planner.setDaemon(true);
        planner.setPriority(Thread.MIN_PRIORITY);
        planner.start();
    }

    private void planAll() {
        for (int n = 1; n <= levels.size(); n++) {
            Autopilot.Route route = Autopilot.demo(levels, n);
            if (route != null) {
                routes.put(n, route);
            }
        }
    }

    boolean running() {
        return demo != null;
    }

    /** The demo being played, or null. */
    Game game() {
        return demo;
    }

    /** Called each tick the title screen sits untouched; starts a demo once it's been long enough. */
    void idleTick() {
        if (++idleTicks >= IDLE_TICKS) {
            start();
        }
    }

    /** Someone pressed a key: start counting again. */
    void resetIdle() {
        idleTicks = 0;
    }

    private void start() {
        idleTicks = 0;
        Autopilot.Route route = routes.get(nextLevel);
        if (route == null) {
            return; // still planning; try again after the next wait
        }
        nextLevel = nextLevel % levels.size() + 1;
        demo = Autopilot.start(levels, route.levelNumber(), route.seed());
        inputs = route.inputs();
        step = 0;
        ticks = 0;
        quicheUntil = -1;
        endTicks = 0;
    }

    void stop() {
        demo = null;
        idleTicks = 0;
    }

    /** Plays one tick of the demo, pressing the keys the route calls for in step with the lad. */
    void tick() {
        if (demo.state() == Game.State.PLAYING && ticks % Game.PLAYER_PERIOD == 0 && step < inputs.size()) {
            Autopilot.apply(demo, inputs.get(step++));
            if (step == inputs.size() / 2) {
                quicheUntil = ticks + QUICHE_TICKS;
            }
        }
        demo.tick();
        ticks++;
        boolean over = demo.state() != Game.State.PLAYING || step >= inputs.size();
        if (over && ++endTicks >= END_TICKS) {
            stop();
        }
    }

    /** True for a few seconds from the halfway point of the route. */
    boolean showQuiche() {
        return demo != null && ticks < quicheUntil;
    }
}
