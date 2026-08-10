package io.quarkus.devshell.deployment.tui.screens;

import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.paragraph.Paragraph;
import io.quarkus.devshell.deployment.DevShellContext;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;

public class MainMenuScreen implements Screen {

    private static final Style SELECTED_STYLE = Style.EMPTY.onCyan().black().bold();
    private static final Style NORMAL_STYLE = Style.EMPTY.white();
    private static final Style HEADER_STYLE = Style.EMPTY.cyan().bold();
    private static final Style DIM_STYLE = Style.EMPTY.gray();

    private final List<MenuItem> menuItems = List.of(
            new MenuItem("Extensions", "View loaded extensions and their status"),
            new MenuItem("Configuration", "View and search application configuration properties"),
            new MenuItem("Endpoints", "Browse HTTP endpoints, static resources and routes"),
            new MenuItem("Continuous Testing", "Monitor and control continuous test execution"),
            new MenuItem("Dev Services", "View running dev services and container details"),
            new MenuItem("Build Metrics", "Build time statistics and step timings"),
            new MenuItem("Dependencies", "Application dependency tree"),
            new MenuItem("Readme", "View project README.md file"),
            new MenuItem("Workspace", "Project workspace overview"));

    private int selectedIndex = 0;
    private AppContext ctx;

    @Override
    public String getTitle() {
        return "Main Menu";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        var areas = Layout.horizontal()
                .constraints(Constraint.percentage(40), Constraint.percentage(60))
                .split(area);

        renderMenuList(areas.get(0), buffer);
        renderDescription(areas.get(1), buffer);
    }

    private void renderMenuList(Rect area, Buffer buffer) {
        Block menuBlock = Block.builder()
                .title(" Menu ")
                .borders(Borders.ALL)
                .build();

        Rect inner = menuBlock.inner(area);
        menuBlock.render(area, buffer);

        for (int i = 0; i < menuItems.size() && i < inner.height(); i++) {
            MenuItem item = menuItems.get(i);
            Style style = i == selectedIndex ? SELECTED_STYLE : NORMAL_STYLE;
            String prefix = i == selectedIndex ? " > " : "   ";
            String text = prefix + item.title;

            if (i == selectedIndex) {
                buffer.fill(new Rect(inner.x(), inner.y() + i, inner.width(), 1), new Cell(" ", SELECTED_STYLE));
            }

            buffer.setString(inner.x(), inner.y() + i, text, style);
        }
    }

    private void renderDescription(Rect area, Buffer buffer) {
        MenuItem selected = menuItems.get(selectedIndex);

        Block descBlock = Block.builder()
                .title(" " + selected.title + " ")
                .borders(Borders.ALL)
                .build();

        Rect inner = descBlock.inner(area);
        descBlock.render(area, buffer);

        Paragraph desc = Paragraph.builder()
                .text(Text.from(
                        Line.from(Span.styled(selected.description, NORMAL_STYLE)),
                        Line.from(),
                        Line.from(Span.styled("Press Enter to open, ESC to quit", DIM_STYLE))))
                .build();
        desc.render(inner, buffer);
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length == 1) {
            switch (keys[0]) {
                case 'k':
                    moveUp();
                    return true;
                case 'j':
                    moveDown();
                    return true;
                case '\r':
                case '\n':
                    openSelected();
                    return true;
            }
        }
        if (keys.length == 3 && keys[0] == 27 && keys[1] == '[') {
            if (keys[2] == 'A') {
                moveUp();
                return true;
            } else if (keys[2] == 'B') {
                moveDown();
                return true;
            }
        }
        return false;
    }

    private void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            ctx.requestRedraw();
        }
    }

    private void moveDown() {
        if (selectedIndex < menuItems.size() - 1) {
            selectedIndex++;
            ctx.requestRedraw();
        }
    }

    private void openSelected() {
        Screen screen = switch (selectedIndex) {
            case 0 -> new ExtensionsListScreen(DevShellContext.getExtensionInfos());
            case 1 -> new ConfigurationScreen();
            case 2 -> new EndpointsScreen();
            case 3 -> new ContinuousTestingScreen();
            case 4 -> new DevServicesScreen();
            case 5 -> new BuildMetricsScreen();
            case 6 -> new DependenciesScreen();
            case 7 -> new ReadmeScreen();
            case 8 -> new WorkspaceScreen();
            default -> null;
        };
        if (screen != null) {
            ctx.navigateTo(screen);
        }
    }

    private record MenuItem(String title, String description) {
    }
}
