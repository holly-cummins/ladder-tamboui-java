package dev.holly.ladder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;

/** Clicks on the title screen land on what's drawn there, in every style and terminal size. */
class ClickTargetTest {

    @Test
    void clickTargetsSitOnTheWordsTheyStandFor() {
        for (String style : Theme.NAMES) {
            for (int[] size : new int[][] {{62, 24}, {64, 24}, {101, 37}}) {
                Renderer r = new Renderer(Theme.named(style));
                Buffer buffer = Buffer.empty(new Rect(0, 0, size[0], size[1]));
                r.draw(Frame.forTesting(buffer), new Game(Level.loadAll(), new Random(1)));
                String where = style + " at " + size[0] + "x" + size[1];
                assertEquals(Theme.NAMES.size() + 1, r.clickTargets().size(), where);
                for (Renderer.ClickTarget t : r.clickTargets()) {
                    String text = text(buffer, t);
                    String expected = t.action().equals("start") ? "Press space to start" : t.action().substring(6);
                    assertTrue(text.contains(expected), where + ": " + t.action() + " target covers \"" + text + "\"");
                    assertEquals(t.action(), r.clickAt(t.x() + t.width() / 2, t.y()));
                }
                assertNull(r.clickAt(0, 0), "the corner does nothing");
            }
        }
    }

    private static String text(Buffer buffer, Renderer.ClickTarget t) {
        StringBuilder sb = new StringBuilder();
        for (int x = t.x(); x < t.x() + t.width(); x++) {
            sb.append(buffer.get(x, t.y()).symbol());
        }
        return sb.toString();
    }
}
