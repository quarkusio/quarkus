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
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;
import io.quarkus.devshell.deployment.tui.pages.ExtensionPageFactory;

public class ExtensionsListScreen implements Screen {

    private static final Style ACTIVE = Style.EMPTY.green();
    private static final Style INACTIVE = Style.EMPTY.gray();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style HEADER = Style.EMPTY.cyan().bold();
    private static final Style DIM = Style.EMPTY.gray();

    private final List<ExtensionInfo> extensions;
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private AppContext ctx;

    public ExtensionsListScreen(List<ExtensionInfo> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String getTitle() {
        return "Extensions";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        long activeCount = extensions.stream().filter(ExtensionInfo::active).count();
        long inactiveCount = extensions.size() - activeCount;

        var areas = Layout.horizontal()
                .constraints(Constraint.percentage(40), Constraint.percentage(60))
                .split(area);

        renderExtensionList(areas.get(0), buffer, activeCount, inactiveCount);
        renderExtensionDetail(areas.get(1), buffer);
    }

    private void renderExtensionList(Rect area, Buffer buffer, long activeCount, long inactiveCount) {
        Block listBlock = Block.builder()
                .title(" Extensions (" + activeCount + " active, " + inactiveCount + " inactive) ")
                .borders(Borders.ALL)
                .build();
        Rect inner = listBlock.inner(area);
        listBlock.render(area, buffer);

        int visibleRows = inner.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < extensions.size(); i++) {
            int idx = scrollOffset + i;
            ExtensionInfo ext = extensions.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(inner.x(), inner.y() + i, inner.width(), 1), new Cell(" ", SELECTED));
            }

            String icon = ext.active ? "* " : "o ";
            Style iconStyle = selected ? SELECTED : (ext.active ? ACTIVE : INACTIVE);
            buffer.setString(inner.x(), inner.y() + i, icon, iconStyle);
            buffer.setString(inner.x() + 2, inner.y() + i,
                    truncate(ext.name, inner.width() - 2), selected ? SELECTED : NORMAL);
        }
    }

    private void renderExtensionDetail(Rect area, Buffer buffer) {
        if (selectedIndex >= extensions.size()) {
            return;
        }
        ExtensionInfo ext = extensions.get(selectedIndex);

        Block detailBlock = Block.builder()
                .title(" " + ext.name + " ")
                .borders(Borders.ALL)
                .build();
        Rect inner = detailBlock.inner(area);
        detailBlock.render(area, buffer);

        int y = inner.y();

        Style statusStyle = ext.active ? ACTIVE : INACTIVE;
        buffer.setString(inner.x(), y, ext.active ? "* Active" : "o Inactive", statusStyle);
        y += 2;

        buffer.setString(inner.x(), y, "Namespace:", HEADER);
        buffer.setString(inner.x() + 11, y, ext.namespace, DIM);
        y += 2;

        if (ext.description != null && !ext.description.isEmpty()) {
            Paragraph desc = Paragraph.builder()
                    .text(Text.from(Line.from(Span.styled(ext.description, NORMAL))))
                    .build();
            Rect descArea = new Rect(inner.x(), y, inner.width(), Math.min(3, inner.height() - (y - inner.y())));
            desc.render(descArea, buffer);
            y += 4;
        }

        if (ext.keywords != null && !ext.keywords.isEmpty()) {
            buffer.setString(inner.x(), y, "Keywords:", HEADER);
            y++;
            buffer.setString(inner.x(), y, truncate(String.join(", ", ext.keywords), inner.width()), DIM);
            y += 2;
        }

        buffer.setString(inner.x(), y, "Press Enter to explore", DIM);
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length == 1) {
            switch (keys[0]) {
                case 'j':
                    return moveDown();
                case 'k':
                    return moveUp();
                case '\r':
                case '\n':
                    if (selectedIndex < extensions.size()) {
                        ExtensionInfo ext = extensions.get(selectedIndex);
                        Screen page = ExtensionPageFactory.createPage(ext, ctx);
                        if (page != null) {
                            ctx.navigateTo(page);
                        }
                    }
                    return true;
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
        if (selectedIndex < extensions.size() - 1) {
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

    public record ExtensionInfo(String namespace, String name, String description, List<String> keywords,
            boolean active) {
        public ExtensionInfo {
            namespace = namespace != null ? namespace : "";
            name = name != null ? name : "";
            description = description != null ? description : "";
            keywords = keywords != null ? keywords : List.of();
        }
    }
}
