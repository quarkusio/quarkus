package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.deployment.tui.widgets.TableView;
import tools.jackson.databind.JsonNode;

public class ArcShellPage extends BaseExtensionPage {

    private final TableView<BeanInfo> beansTable = new TableView<>();
    private final TableView<ObserverInfo> observersTable = new TableView<>();
    private final TableView<BeanInfo> removedTable = new TableView<>();

    private final List<BeanInfo> beans = new ArrayList<>();
    private final List<ObserverInfo> observers = new ArrayList<>();
    private final List<BeanInfo> removedBeans = new ArrayList<>();

    public ArcShellPage() {
        super("quarkus-arc", "ArC CDI");
        setTabs("Beans", "Observers", "Removed");

        beansTable.addColumn("Bean Class", b -> b.beanClass, 30);
        beansTable.addColumn("Scope", b -> b.scope, 15);
        beansTable.addColumn("Kind", b -> b.kind, 12);

        observersTable.addColumn("Observer Class", o -> o.declaringClass, 30);
        observersTable.addColumn("Observed Type", o -> o.observedType, 25);
        observersTable.addColumn("Priority", o -> String.valueOf(o.priority), 10);

        removedTable.addColumn("Bean Class", b -> b.beanClass, 30);
        removedTable.addColumn("Scope", b -> b.scope, 15);
        removedTable.addColumn("Kind", b -> b.kind, 12);
    }

    @Override
    public void loadData() {
        // Beans (build-time data)
        JsonNode beansResult = getBuildTimeDataAsJson("beans");
        beans.clear();
        if (beansResult.isArray()) {
            for (JsonNode b : beansResult) {
                beans.add(new BeanInfo(
                        b.path("beanClass").asText(b.path("providerType").path("name").asText("")),
                        b.path("scope").asText(b.path("scope").path("simpleName").asText("")),
                        b.path("kind").asText("")));
            }
        }
        beansTable.setItems(beans);

        // Observers (build-time data)
        JsonNode observersResult = getBuildTimeDataAsJson("observers");
        observers.clear();
        if (observersResult.isArray()) {
            for (JsonNode o : observersResult) {
                observers.add(new ObserverInfo(
                        o.path("declaringClass").asText(o.path("declaringClass").path("simpleName").asText("")),
                        o.path("observedType").asText(o.path("observedType").path("name").asText("")),
                        o.path("priority").asInt(0)));
            }
        }
        observersTable.setItems(observers);

        // Removed beans (build-time data)
        JsonNode removedResult = getBuildTimeDataAsJson("removedBeans");
        removedBeans.clear();
        if (removedResult.isArray()) {
            for (JsonNode b : removedResult) {
                removedBeans.add(new BeanInfo(
                        b.path("beanClass").asText(b.path("providerType").path("name").asText("")),
                        b.path("scope").asText(b.path("scope").path("simpleName").asText("")),
                        b.path("kind").asText("")));
            }
        }
        removedTable.setItems(removedBeans);
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        int x = area.x() + 1;
        int y = area.y();
        int w = area.width() - 2;
        int h = area.height() - 1;

        switch (getActiveTab()) {
            case 0:
                buffer.setString(x, y, beans.size() + " beans", DIM_STYLE);
                beansTable.setVisibleRows(h - 3);
                beansTable.setWidth(w);
                beansTable.render(buffer, y + 1, x);
                break;
            case 1:
                buffer.setString(x, y, observers.size() + " observers", DIM_STYLE);
                observersTable.setVisibleRows(h - 3);
                observersTable.setWidth(w);
                observersTable.render(buffer, y + 1, x);
                break;
            case 2:
                buffer.setString(x, y, removedBeans.size() + " removed beans", DIM_STYLE);
                removedTable.setVisibleRows(h - 3);
                removedTable.setWidth(w);
                removedTable.render(buffer, y + 1, x);
                break;
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                activeTable().moveDown();
                ctx.requestRedraw();
                return true;
            case 'k':
            case KeyCode.UP:
                activeTable().moveUp();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_DOWN:
                activeTable().pageDown();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                activeTable().pageUp();
                ctx.requestRedraw();
                return true;
            default:
                return false;
        }
    }

    private TableView<?> activeTable() {
        switch (getActiveTab()) {
            case 1:
                return observersTable;
            case 2:
                return removedTable;
            default:
                return beansTable;
        }
    }

    @Override
    protected void onTabChanged(int newTab) {
        // Tables keep their own scroll state, nothing extra needed
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate";
    }

    private static class BeanInfo {
        final String beanClass;
        final String scope;
        final String kind;

        BeanInfo(String beanClass, String scope, String kind) {
            this.beanClass = beanClass;
            this.scope = scope;
            this.kind = kind;
        }
    }

    private static class ObserverInfo {
        final String declaringClass;
        final String observedType;
        final int priority;

        ObserverInfo(String declaringClass, String observedType, int priority) {
            this.declaringClass = declaringClass;
            this.observedType = observedType;
            this.priority = priority;
        }
    }
}
