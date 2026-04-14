package io.quarkus.devshell.runtime.tui;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.devshell.runtime.DevShellRouter;
import io.quarkus.devshell.runtime.ReflectiveBuildTimeDataReader;

/**
 * Application context passed to screens providing access to
 * router, navigation, and application state — but NOT rendering.
 * Rendering goes through TamboUI's Frame/Buffer system.
 */
public class AppContext {

    private final TerminalUI terminalUI;
    private final DevShellRouter router;
    private int width;
    private int height;

    public AppContext(TerminalUI terminalUI, DevShellRouter router, int width, int height) {
        this.terminalUI = terminalUI;
        this.router = router;
        this.width = width;
        this.height = height;
    }

    /**
     * Get terminal width in characters.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get terminal height in lines.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Update terminal dimensions.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Get the JSON-RPC router for calling backend methods.
     */
    public DevShellRouter getRouter() {
        return router;
    }

    /**
     * Navigate to a new screen.
     */
    public void navigateTo(Screen screen) {
        terminalUI.navigateTo(screen);
    }

    /**
     * Go back to the previous screen.
     *
     * @return true if went back, false if already at root
     */
    public boolean goBack() {
        return terminalUI.goBack();
    }

    /**
     * Exit the TUI and return to normal console.
     */
    public void exit() {
        terminalUI.stop();
    }

    /**
     * Request a screen redraw.
     */
    public void requestRedraw() {
        terminalUI.requestRedraw();
    }

    /**
     * Set the status bar message.
     */
    public void setStatus(String message) {
        terminalUI.setStatus(message);
    }

    /**
     * Get available JSON-RPC runtime methods.
     */
    public Map<String, ?> getRuntimeMethods() {
        return router.getRuntimeMethods();
    }

    /**
     * Get available JSON-RPC deployment methods.
     */
    public Map<String, ?> getDeploymentMethods() {
        return router.getDeploymentMethods();
    }

    /**
     * Get the list of extensions.
     */
    public List<ShellExtension> getExtensions() {
        return terminalUI.getExtensions();
    }

    /**
     * Get the shell page info map.
     */
    public Map<String, ShellPageInfo> getShellPages() {
        return terminalUI.getShellPages();
    }

    /**
     * Get all build-time data for a given extension namespace.
     *
     * @param namespace the extension namespace (e.g., "quarkus-arc")
     * @return a map of field names to their JSON string values
     */
    public Map<String, String> getBuildTimeData(String namespace) {
        ReflectiveBuildTimeDataReader reader = terminalUI.getBuildTimeDataReader();
        if (reader != null) {
            return reader.getBuildTimeData(namespace);
        }
        return Collections.emptyMap();
    }

    /**
     * Get a specific build-time data field for an extension.
     *
     * @param namespace the extension namespace (e.g., "quarkus-arc")
     * @param fieldName the field name (e.g., "beans")
     * @return the JSON string value, or null if not found
     */
    public String getBuildTimeDataField(String namespace, String fieldName) {
        ReflectiveBuildTimeDataReader reader = terminalUI.getBuildTimeDataReader();
        if (reader != null) {
            return reader.getBuildTimeDataField(namespace, fieldName);
        }
        return null;
    }
}
