package io.quarkus.devshell.deployment.tui;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * A full-screen TUI view. Screens are stacked: navigating to a new screen
 * pushes it onto the stack, pressing Escape/back pops it.
 */
public interface Screen {

    String getTitle();

    default void onEnter(AppContext ctx) {
    }

    default void onLeave() {
    }

    void render(Rect area, Buffer buffer);

    /**
     * Handle a keyboard event.
     *
     * @return true if the key was consumed, false to let the shell handle it
     */
    boolean handleKey(int[] keys);

    default void onResize(int width, int height) {
    }

    default void tick() {
    }
}
