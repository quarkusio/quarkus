package io.quarkus.devshell.runtime.tui.screens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.arc.Arc;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.widgets.ListView;

/**
 * Main menu screen showing the Dev UI style navigation.
 */
public class MainMenuScreen implements Screen {

    private AppContext ctx;

    private enum MenuItem {
        EXTENSIONS("Extensions", "Browse installed extensions"),
        CONFIGURATION("Configuration", "View and edit configuration"),
        WORKSPACE("Workspace", "View project workspace information"),
        ENDPOINTS("Endpoints", "View registered endpoints"),
        CONTINUOUS_TESTING("Continuous Testing", "Manage continuous testing"),
        DEV_SERVICES("Dev Services", "Manage dev services"),
        BUILD_METRICS("Build Metrics", "Build steps and timing information"),
        README("Readme", "View project README"),
        DEPENDENCIES("Dependencies", "View project dependencies");

        private final String label;
        private final String description;

        MenuItem(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private final ListView<MenuItem> menuList;
    private int panelWidth = 30;

    public MainMenuScreen() {
        this.menuList = new ListView<>(item -> item.label);
        List<MenuItem> items = new ArrayList<>();
        for (MenuItem item : MenuItem.values()) {
            items.add(item);
        }
        this.menuList.setItems(items);
    }

    @Override
    public String getTitle() {
        return "Quarkus Dev Shell";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        int width = ctx.getWidth();
        panelWidth = Math.max(25, width / 3);
        menuList.setWidth(panelWidth - 2);
        menuList.setVisibleRows(ctx.getHeight() - 6);
    }

    @Override
    public void onLeave() {
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        BufferHelper.writeHeader(buffer, " QUARKUS DEV SHELL ", width);
        buffer.setString(1, 1, "Terminal interface for Quarkus Dev Mode", Style.create().gray());

        // Left panel header
        buffer.setString(0, 3, " Menu", Style.create().cyan().bold());
        BufferHelper.writeLine(buffer, 1, 4, panelWidth - 2, Style.create().gray());

        // Menu list
        menuList.render(buffer, 5, 0);

        // Right panel
        MenuItem selected = menuList.getSelectedItem();
        if (selected != null) {
            int rps = panelWidth + 1;
            int row = 3;

            buffer.setString(rps, row, selected.label, Style.create().bold().cyan());
            row++;
            buffer.setString(rps, row, "\u2550".repeat(Math.min(selected.label.length(), width - rps - 1)),
                    Style.create().gray());
            row += 2;
            buffer.setString(rps, row, selected.description, Style.create().white());
            row += 2;

            if (selected == MenuItem.EXTENSIONS) {
                List<ShellExtension> extensions = this.ctx.getExtensions();
                long activeCount = extensions.stream().filter(ShellExtension::isActive).count();
                long inactiveCount = extensions.size() - activeCount;

                buffer.setString(rps, row, "Active: ", Style.create().cyan());
                buffer.setString(rps + 8, row, String.valueOf(activeCount), Style.create().green());
                row++;
                buffer.setString(rps, row, "Inactive: ", Style.create().cyan());
                buffer.setString(rps + 10, row, String.valueOf(inactiveCount), Style.create().gray());
                row += 2;
                buffer.setString(rps, row, "Press Enter to browse extensions", Style.create().gray());
            }
        }

        // Divider
        BufferHelper.writeDivider(buffer, panelWidth - 1, 3, height - 2);
    }

    @Override
    public boolean handleKey(int key) {
        if (menuList.handleKey(key)) {
            this.ctx.requestRedraw();
            return true;
        }

        switch (key) {
            case KeyCode.ENTER:
                MenuItem selected = menuList.getSelectedItem();
                if (selected != null) {
                    openMenuItem(selected);
                }
                return true;

            case KeyCode.ESCAPE:
                this.ctx.exit();
                return true;

            default:
                return false;
        }
    }

    private void openMenuItem(MenuItem item) {
        switch (item) {
            case EXTENSIONS:
                List<ShellExtension> extensions = new ArrayList<>(this.ctx.getExtensions());
                extensions.sort(Comparator
                        .comparing((ShellExtension e) -> !e.isActive())
                        .thenComparing(ShellExtension::getDisplayName, String.CASE_INSENSITIVE_ORDER));
                this.ctx.navigateTo(new ExtensionsListScreen(extensions));
                break;
            case CONFIGURATION:
                this.ctx.navigateTo(cdiScreen(ConfigurationScreen.class));
                break;
            case WORKSPACE:
                this.ctx.navigateTo(new WorkspaceScreen());
                break;
            case ENDPOINTS:
                this.ctx.navigateTo(cdiScreen(EndpointsScreen.class));
                break;
            case CONTINUOUS_TESTING:
                this.ctx.navigateTo(cdiScreen(ContinuousTestingScreen.class));
                break;
            case DEV_SERVICES:
                this.ctx.navigateTo(new DevServicesScreen());
                break;
            case BUILD_METRICS:
                this.ctx.navigateTo(cdiScreen(BuildInfoScreen.class));
                break;
            case README:
                this.ctx.navigateTo(new ReadmeScreen());
                break;
            case DEPENDENCIES:
                this.ctx.navigateTo(new DependenciesScreen());
                break;
        }
    }

    private <T extends Screen> T cdiScreen(Class<T> screenClass) {
        return Arc.container().instance(screenClass).get();
    }

    @Override
    public void onResize(int width, int height) {
        panelWidth = Math.max(25, width / 3);
        menuList.setWidth(panelWidth - 2);
        menuList.setVisibleRows(height - 6);
    }
}
