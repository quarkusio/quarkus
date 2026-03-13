package io.quarkus.devshell.runtime.tui.pages;

import dev.tamboui.buffer.Buffer;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.Screen;
import io.quarkus.devshell.runtime.tui.ShellExtension;

/**
 * Interface for extension-specific pages.
 * Each extension can have its own page implementation that knows
 * how to fetch and display its data in a meaningful way.
 */
public interface ExtensionPage extends Screen {

    /**
     * Get the extension this page is for.
     */
    ShellExtension getExtension();

    /**
     * Called to load/refresh data for this page.
     * Implementations should call the appropriate JSON-RPC methods
     * and update their internal state.
     */
    void loadData();

    /**
     * Render this page's content within a bounded panel region.
     * Used when embedding the page in another screen (e.g., right panel of extensions list).
     *
     * @param buffer the buffer to render into
     * @param startRow the first row to render at (0-based)
     * @param startCol the first column to render at (0-based)
     * @param panelWidth the available width
     * @param panelHeight the available height
     */
    void renderPanel(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight);

    /**
     * Handle a key event when embedded in a panel.
     * Returns true if the key was handled.
     */
    boolean handlePanelKey(int key);

    /**
     * Initialize for panel mode rendering without triggering a full screen enter.
     */
    void initPanel(AppContext ctx);

    /**
     * Reset the page state to force reload on next render.
     */
    void reset();
}
