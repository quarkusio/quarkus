package io.quarkus.devshell.deployment.tui;

import java.util.List;
import java.util.Map;

import io.quarkus.devshell.deployment.DevShellContext;
import io.quarkus.devshell.deployment.DevShellJsonRpcClient;
import io.quarkus.devshell.deployment.tui.screens.ExtensionsListScreen;

/**
 * Context available to all screens, providing access to the JSON-RPC client,
 * navigation, and terminal state.
 */
public class AppContext {

    private final DevShellJsonRpcClient jsonRpcClient;
    private final TerminalUI tui;
    private volatile String statusText;

    AppContext(DevShellJsonRpcClient jsonRpcClient, TerminalUI tui) {
        this.jsonRpcClient = jsonRpcClient;
        this.tui = tui;
    }

    public DevShellJsonRpcClient getJsonRpcClient() {
        return jsonRpcClient;
    }

    public void navigateTo(Screen screen) {
        tui.navigateTo(screen);
    }

    public void goBack() {
        tui.goBack();
    }

    public void exit() {
        tui.exit();
    }

    public int getWidth() {
        return tui.getWidth();
    }

    public int getHeight() {
        return tui.getHeight();
    }

    public void requestRedraw() {
        tui.requestRedraw();
    }

    public void setStatus(String text) {
        this.statusText = text;
        tui.requestRedraw();
    }

    public String getStatus() {
        return statusText;
    }

    public List<ExtensionsListScreen.ExtensionInfo> getExtensions() {
        return DevShellContext.getExtensionInfos();
    }

    public Map<String, DevShellContext.ShellPageInfo> getShellPages() {
        return DevShellContext.getShellPages();
    }
}
