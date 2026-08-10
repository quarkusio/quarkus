package io.quarkus.devshell.deployment.tui.pages;

import java.util.concurrent.CompletableFuture;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.DevShellContext;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.KeyCode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public abstract class BaseExtensionPage implements ExtensionPage {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Style TAB_ACTIVE = Style.EMPTY.bold().reversed();
    private static final Style TAB_INACTIVE = Style.EMPTY.gray();
    private static final Style LOADING_STYLE = Style.EMPTY.yellow();
    private static final Style ERROR_STYLE = Style.EMPTY.red();
    protected static final Style FOOTER_STYLE = Style.EMPTY.gray();
    protected static final Style HEADER_STYLE = Style.EMPTY.cyan().bold();
    protected static final Style LABEL_STYLE = Style.EMPTY.cyan();
    protected static final Style VALUE_STYLE = Style.EMPTY.white();
    protected static final Style DIM_STYLE = Style.EMPTY.gray();

    protected final String namespace;
    protected final String displayName;
    protected AppContext ctx;
    protected volatile boolean loading = false;
    protected volatile String errorMessage;
    protected boolean initialized = false;

    private String[] tabNames;
    private int activeTabIndex = 0;

    protected BaseExtensionPage(String namespace, String displayName) {
        this.namespace = namespace;
        this.displayName = displayName;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getTitle() {
        return displayName;
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        if (!initialized) {
            initialized = true;
            loadDataAsync();
        }
    }

    @Override
    public void onLeave() {
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (loading) {
            buffer.setString(area.x() + 2, area.y() + 1, "Loading...", LOADING_STYLE);
            return;
        }
        if (errorMessage != null) {
            buffer.setString(area.x() + 2, area.y() + 1, "Error: " + truncate(errorMessage, area.width() - 10),
                    ERROR_STYLE);
            return;
        }

        if (tabNames != null && tabNames.length > 1) {
            var areas = Layout.vertical()
                    .constraints(Constraint.length(2), Constraint.fill(), Constraint.length(1))
                    .split(area);
            renderTabBar(areas.get(0), buffer);
            renderPanel(areas.get(1), buffer);
            renderFooterBar(areas.get(2), buffer);
        } else {
            var areas = Layout.vertical()
                    .constraints(Constraint.fill(), Constraint.length(1))
                    .split(area);
            renderPanel(areas.get(0), buffer);
            renderFooterBar(areas.get(1), buffer);
        }
    }

    @Override
    public boolean handleKey(int[] keys) {
        int key = KeyCode.parse(keys);

        if (tabNames != null && tabNames.length > 1) {
            if (key == KeyCode.TAB) {
                activeTabIndex = (activeTabIndex + 1) % tabNames.length;
                onTabChanged(activeTabIndex);
                ctx.requestRedraw();
                return true;
            }
            if (key >= '1' && key <= '9') {
                int idx = key - '1';
                if (idx < tabNames.length) {
                    activeTabIndex = idx;
                    onTabChanged(activeTabIndex);
                    ctx.requestRedraw();
                    return true;
                }
            }
        }

        if (key == 'r' || key == 'R') {
            loadDataAsync();
            return true;
        }

        return handlePanelKey(keys);
    }

    @Override
    public void initPanel(AppContext ctx) {
        if (!initialized) {
            this.ctx = ctx;
            initialized = true;
            loadDataAsync();
        }
    }

    @Override
    public void reset() {
        initialized = false;
        loading = false;
        errorMessage = null;
    }

    // --- Tab support ---

    protected void setTabs(String... names) {
        this.tabNames = names;
        if (activeTabIndex >= names.length) {
            activeTabIndex = 0;
        }
    }

    protected int getActiveTab() {
        return activeTabIndex;
    }

    protected String getActiveTabName() {
        if (tabNames != null && activeTabIndex < tabNames.length) {
            return tabNames[activeTabIndex];
        }
        return null;
    }

    protected void onTabChanged(int newTab) {
    }

    private void renderTabBar(Rect area, Buffer buffer) {
        int col = area.x() + 1;
        for (int i = 0; i < tabNames.length; i++) {
            String label = " " + tabNames[i] + " ";
            buffer.setString(col, area.y(), label, i == activeTabIndex ? TAB_ACTIVE : TAB_INACTIVE);
            col += label.length() + 1;
        }
    }

    // --- Data loading ---

    protected void loadDataAsync() {
        loading = true;
        errorMessage = null;
        if (ctx != null) {
            ctx.requestRedraw();
        }
        CompletableFuture.runAsync(() -> {
            try {
                loadData();
                loading = false;
            } catch (Exception e) {
                loading = false;
                errorMessage = extractErrorMessage(e);
            }
            if (ctx != null) {
                ctx.requestRedraw();
            }
        });
    }

    private String extractErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return e.getClass().getSimpleName();
        }
        // Strip internal JSON-RPC method names (namespace_method format)
        if (msg.contains("failed:")) {
            String detail = msg.substring(msg.indexOf("failed:") + 7).trim();
            if (!detail.isEmpty()) {
                return detail;
            }
        }
        // Strip "JSON-RPC error NNNN: " prefix
        if (msg.startsWith("JSON-RPC error ")) {
            int colonIdx = msg.indexOf(':', 15);
            if (colonIdx > 0 && colonIdx < msg.length() - 1) {
                return msg.substring(colonIdx + 1).trim();
            }
        }
        return msg;
    }

    // --- JSON-RPC helpers ---

    protected JsonNode rpcCall(String method) {
        return ctx.getJsonRpcClient().call(namespace, method);
    }

    protected JsonNode rpcCall(String method, ObjectNode params) {
        return ctx.getJsonRpcClient().call(namespace, method, params);
    }

    protected CompletableFuture<JsonNode> rpcCallAsync(String method) {
        return ctx.getJsonRpcClient().callAsync(namespace, method);
    }

    protected ObjectNode createParams() {
        return MAPPER.createObjectNode();
    }

    @SuppressWarnings("unchecked")
    protected <T> T getBuildTimeData(String key) {
        return (T) DevShellContext.getBuildTimeData(namespace, key);
    }

    protected JsonNode getBuildTimeDataAsJson(String key) {
        Object data = DevShellContext.getBuildTimeData(namespace, key);
        if (data == null) {
            return MAPPER.nullNode();
        }
        return MAPPER.valueToTree(data);
    }

    // --- Rendering helpers ---

    protected void renderFooterBar(Rect area, Buffer buffer) {
        String tabHint = (tabNames != null && tabNames.length > 1) ? "[Tab] Switch  " : "";
        String footer = " " + tabHint + getFooterText() + "  [R] Refresh  [ESC] Back";
        buffer.setString(area.x(), area.y(), truncate(footer, area.width()), FOOTER_STYLE);
    }

    protected String getFooterText() {
        return "";
    }

    protected void renderLoading(Buffer buffer, int x, int y) {
        buffer.setString(x, y, "Loading...", LOADING_STYLE);
    }

    protected void renderError(Buffer buffer, int x, int y, int maxWidth) {
        if (errorMessage != null) {
            buffer.setString(x, y, truncate("Error: " + errorMessage, maxWidth), ERROR_STYLE);
        }
    }

    protected static String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        if (maxLen <= 0)
            return "";
        if (text.length() <= maxLen)
            return text;
        return text.substring(0, maxLen - 1) + "…";
    }

    // --- Panel rendering (for embedding in ExtensionsListScreen detail view) ---

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (loading) {
            buffer.setString(area.x(), area.y(), "Loading...", LOADING_STYLE);
            return;
        }
        if (errorMessage != null) {
            buffer.setString(area.x(), area.y(), truncate(errorMessage, area.width()), ERROR_STYLE);
            return;
        }
        buffer.setString(area.x(), area.y(), "Press Enter to view details", FOOTER_STYLE);
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        return false;
    }
}
