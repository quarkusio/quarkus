package io.quarkus.devshell.runtime.tui.pages;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.JsonHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class BaseExtensionPage implements ExtensionPage {

    private static final Style STYLE_TAB_ACTIVE = Style.create().bold().reversed();
    private static final Style STYLE_TAB_INACTIVE = Style.create().gray();
    private static final Style STYLE_LOADING = Style.create().yellow();
    private static final Style STYLE_ERROR = Style.create().red();
    private static final Style STYLE_FOOTER = Style.create().gray();

    protected ShellExtension extension;
    protected AppContext ctx;
    protected boolean loading = false;
    protected String error = null;
    protected int width = 80;
    protected int height = 24;

    private String[] tabNames;
    private int currentTabIndex = 0;
    private boolean tabArrowNavigation = false;

    protected int panelStartRow = 0;
    protected int panelStartCol = 0;
    protected int panelWidth = 80;
    protected int panelHeight = 24;
    protected boolean initialized = false;

    protected BaseExtensionPage() {
    }

    public BaseExtensionPage(ShellExtension extension) {
        this.extension = extension;
    }

    public void setExtension(ShellExtension extension) {
        this.extension = extension;
    }

    @Override
    public ShellExtension getExtension() {
        return extension;
    }

    @Override
    public String getTitle() {
        return extension.getDisplayName();
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        this.width = ctx.getWidth();
        this.height = ctx.getHeight();
        loadData();
    }

    @Override
    public void onLeave() {
    }

    @Override
    public void onResize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean handleKey(int key) {
        if (tabNames != null) {
            if (key == KeyCode.TAB) {
                nextTab();
                redraw();
                return true;
            }
            if (tabArrowNavigation && key == KeyCode.RIGHT) {
                nextTab();
                redraw();
                return true;
            }
            if (tabArrowNavigation && key == KeyCode.LEFT) {
                prevTab();
                redraw();
                return true;
            }
            if (key >= '1' && key <= '9') {
                int idx = key - '1';
                if (idx < tabNames.length) {
                    currentTabIndex = idx;
                    redraw();
                    return true;
                }
            }
        }

        switch (key) {
            case KeyCode.ESCAPE:
                ctx.goBack();
                return true;
            case 'r':
            case 'R':
                loadData();
                return true;
            default:
                return false;
        }
    }

    public void initPanel(AppContext ctx) {
        if (!initialized) {
            this.ctx = ctx;
            this.initialized = true;
            loadData();
        }
    }

    @Override
    public void renderPanel(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        this.panelStartRow = startRow;
        this.panelStartCol = startCol;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;

        if (!initialized) {
            initialized = true;
            loadData();
        }

        renderPanelContent(buffer, startRow, startCol, panelWidth, panelHeight);
    }

    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            buffer.setString(startCol, row, "Loading...", STYLE_LOADING);
            return;
        }

        if (error != null) {
            buffer.setString(startCol, row, truncate(error, panelWidth - 2), STYLE_ERROR);
            return;
        }

        buffer.setString(startCol, row, "Press Enter to view details", STYLE_FOOTER);
    }

    @Override
    public boolean handlePanelKey(int key) {
        switch (key) {
            case 'r':
            case 'R':
                initialized = false;
                loadData();
                return true;
            default:
                return false;
        }
    }

    protected void renderHeader(Buffer buffer, int width) {
        String title = " " + extension.getDisplayName() + " ";
        int padding = (width - title.length()) / 2;

        Style headerStyle = Style.create().white().onBlue().bold();
        String headerLine = " ".repeat(Math.max(0, padding)) + title
                + " ".repeat(Math.max(0, width - padding - title.length()));
        buffer.setString(0, 0, headerLine, headerStyle);
    }

    protected void renderLoading(Buffer buffer, int row) {
        buffer.setString(1, row, "Loading...", STYLE_LOADING);
    }

    protected void renderPanelLoading(Buffer buffer, int row, int col) {
        buffer.setString(col, row, "Loading...", STYLE_LOADING);
    }

    protected void renderError(Buffer buffer, int row) {
        if (error != null) {
            buffer.setString(1, row, "Error: " + truncate(error, width - 10), STYLE_ERROR);
        }
    }

    protected void renderPanelError(Buffer buffer, int row, int col, int maxWidth) {
        if (error != null) {
            buffer.setString(col, row, truncate(error, maxWidth - 2), STYLE_ERROR);
        }
    }

    /** When tabs are configured, "[Tab] Switch" is automatically prepended. */
    protected void renderFooter(Buffer buffer, String instructions) {
        String prefix = tabNames != null ? "[Tab] Switch  " : "";
        String footerText = prefix + instructions + "  [R] Refresh  [Esc] Back";
        buffer.setString(1, height - 2, footerText, STYLE_FOOTER);
    }

    protected CompletableFuture<String> callMethod(String methodName) {
        return ctx.getRouter().call(methodName, Map.of());
    }

    protected CompletableFuture<String> callMethod(String methodName, Map<String, Object> params) {
        return ctx.getRouter().call(methodName, params);
    }

    protected String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        if (text.length() <= maxLen)
            return text;
        return text.substring(0, maxLen - 1) + "\u2026";
    }

    /** Extracts the "result" field from a JSON-RPC response envelope. */
    protected String extractResult(String jsonRpcResponse) {
        String result = JsonHelper.extractResult(jsonRpcResponse);
        if (result == null) {
            String errorMsg = JsonHelper.extractError(jsonRpcResponse);
            if (errorMsg != null) {
                error = errorMsg;
            }
        }
        return result;
    }

    protected JsonObject extractResultAsJsonObject(String jsonRpcResponse) {
        String result = extractResult(jsonRpcResponse);
        if (result == null || result.isEmpty()) {
            return null;
        }
        try {
            return createJsonObject(result);
        } catch (Exception e) {
            error = "Failed to parse JSON: " + e.getMessage();
            return null;
        }
    }

    protected JsonArray extractResultAsJsonArray(String jsonRpcResponse) {
        String result = extractResult(jsonRpcResponse);
        if (result == null || result.isEmpty()) {
            return null;
        }
        try {
            return createJsonArray(result);
        } catch (Exception e) {
            error = "Failed to parse JSON: " + e.getMessage();
            return null;
        }
    }

    /**
     * Executes a function with the Vert.x classloader as TCCL to avoid
     * ServiceConfigurationError when QuarkusJacksonFactory is discovered
     * from the application classloader but JsonFactory is from a different classloader.
     */
    private static <T> T withVertxClassLoader(Class<?> vertxClass, Function<Void, T> factory) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(vertxClass.getClassLoader());
            return factory.apply(null);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private static JsonObject createJsonObject(String json) {
        return withVertxClassLoader(JsonObject.class, v -> new JsonObject(json));
    }

    private static JsonArray createJsonArray(String json) {
        return withVertxClassLoader(JsonArray.class, v -> new JsonArray(json));
    }

    /** Can be called multiple times to update tab names dynamically. */
    protected void setTabs(String... names) {
        this.tabNames = names;
        if (currentTabIndex >= names.length) {
            currentTabIndex = 0;
        }
    }

    protected boolean hasTabs() {
        return tabNames != null && tabNames.length > 0;
    }

    protected int getCurrentTabIndex() {
        return currentTabIndex;
    }

    protected String getCurrentTabName() {
        if (tabNames != null && currentTabIndex < tabNames.length) {
            return tabNames[currentTabIndex];
        }
        return null;
    }

    protected void setCurrentTabIndex(int index) {
        if (tabNames != null && index >= 0 && index < tabNames.length) {
            this.currentTabIndex = index;
        }
    }

    /** Disabled by default because arrow keys are often used by list/table widgets. */
    protected void setTabArrowNavigation(boolean enabled) {
        this.tabArrowNavigation = enabled;
    }

    /** Renders the tab bar and returns the next available row (row + 2). */
    protected int renderTabBar(Buffer buffer, int row) {
        if (tabNames == null) {
            return row;
        }

        int col = 1;
        for (int i = 0; i < tabNames.length; i++) {
            String label = " " + tabNames[i] + " ";
            buffer.setString(col, row, label, i == currentTabIndex ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
            col += label.length() + 1;
        }

        return row + 2;
    }

    private void nextTab() {
        if (tabNames != null) {
            currentTabIndex = (currentTabIndex + 1) % tabNames.length;
        }
    }

    private void prevTab() {
        if (tabNames != null) {
            currentTabIndex = (currentTabIndex - 1 + tabNames.length) % tabNames.length;
        }
    }

    protected void redraw() {
        if (ctx != null) {
            ctx.requestRedraw();
        }
    }

    protected String getBuildTimeData(String fieldName) {
        if (ctx == null || extension == null) {
            return null;
        }
        return ctx.getBuildTimeDataField(extension.namespace(), fieldName);
    }

    protected String getBuildTimeData(String namespace, String fieldName) {
        if (ctx == null) {
            return null;
        }
        return ctx.getBuildTimeDataField(namespace, fieldName);
    }

    public void reset() {
        initialized = false;
        loading = false;
        error = null;
    }
}
