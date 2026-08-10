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
import io.quarkus.devshell.deployment.DevShellContext;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;

public class DependenciesScreen implements Screen {

    private static final Style DIM = Style.EMPTY.gray();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style GREEN = Style.EMPTY.green();
    private static final Style CYAN = Style.EMPTY.cyan();

    private AppContext ctx;
    private List<DependencyInfo> deps = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    @Override
    public String getTitle() {
        return "Dependencies";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        deps = DevShellContext.getDependencyInfos();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        var areas = Layout.vertical()
                .constraints(Constraint.length(1), Constraint.fill(), Constraint.length(1))
                .split(area);

        buffer.setLine(areas.get(0).x(), areas.get(0).y(), Line.from(
                Span.styled(" " + deps.size() + " dependencies", DIM)));

        renderList(areas.get(1), buffer);
        renderFooter(areas.get(2), buffer);
    }

    private void renderList(Rect area, Buffer buffer) {
        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < deps.size(); i++) {
            int idx = scrollOffset + i;
            DependencyInfo dep = deps.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            Style aStyle = selected ? SELECTED : NORMAL;
            Style vStyle = selected ? SELECTED : GREEN;
            Style sStyle = selected ? SELECTED : CYAN;

            int x = area.x() + 1;
            String groupArt = dep.groupId + ":" + dep.artifactId;
            int nameWidth = area.width() * 2 / 3;
            buffer.setString(x, area.y() + i, truncate(groupArt, nameWidth), aStyle);
            x += nameWidth + 1;
            buffer.setString(x, area.y() + i, truncate(dep.version, 15), vStyle);
            x += 16;
            if (!dep.scope.isEmpty()) {
                buffer.setString(x, area.y() + i, dep.scope, sStyle);
            }
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length == 1) {
            switch (keys[0]) {
                case 'j':
                    return moveDown();
                case 'k':
                    return moveUp();
            }
        }
        if (keys.length == 3 && keys[0] == 27 && keys[1] == '[') {
            if (keys[2] == 'A')
                return moveUp();
            if (keys[2] == 'B')
                return moveDown();
        }
        return false;
    }

    private boolean moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private boolean moveDown() {
        if (selectedIndex < deps.size() - 1) {
            selectedIndex++;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 1) + "~";
    }

    public record DependencyInfo(String groupId, String artifactId, String version, String scope) {
    }
}
