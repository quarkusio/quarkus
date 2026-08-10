package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.deployment.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.deployment.tui.widgets.ListView;
import tools.jackson.databind.JsonNode;

public class HibernateOrmShellPage extends BaseExtensionPage {

    private int persistenceUnits = 0;
    private int entityTypes = 0;
    private int namedQueries = 0;

    private final ListView<String> puListView = new ListView<>(name -> name);
    private final ListView<String> entityListView = new ListView<>(name -> name);
    private final List<String> puNames = new ArrayList<>();
    private final List<String> entityNames = new ArrayList<>();

    public HibernateOrmShellPage() {
        super("quarkus-hibernate-orm", "Hibernate ORM");
        setTabs("Info", "Persistence Units", "Entities");
    }

    @Override
    public void loadData() {
        JsonNode puResult = rpcCall("getNumberOfPersistenceUnits");
        persistenceUnits = puResult.asInt(0);

        JsonNode entityResult = rpcCall("getNumberOfEntityTypes");
        entityTypes = entityResult.asInt(0);

        JsonNode queryResult = rpcCall("getNumberOfNamedQueries");
        namedQueries = queryResult.asInt(0);

        // Try to get detailed lists if available
        puNames.clear();
        try {
            JsonNode puList = rpcCall("getPersistenceUnits");
            if (puList.isArray()) {
                for (JsonNode pu : puList) {
                    puNames.add(pu.path("name").asText(pu.asText("")));
                }
            }
        } catch (Exception ignored) {
            // Method may not exist, just show counts
        }
        puListView.setItems(puNames);

        entityNames.clear();
        try {
            JsonNode entityList = rpcCall("getEntityTypes");
            if (entityList.isArray()) {
                for (JsonNode e : entityList) {
                    entityNames.add(e.path("name").asText(e.path("className").asText(e.asText(""))));
                }
            }
        } catch (Exception ignored) {
            // Method may not exist, just show counts
        }
        entityListView.setItems(entityNames);
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        int x = area.x() + 1;
        int y = area.y();
        int w = area.width() - 2;
        int h = area.height();

        switch (getActiveTab()) {
            case 0:
                KeyValuePanel info = new KeyValuePanel("Hibernate ORM Summary");
                info.add("Persistence Units", String.valueOf(persistenceUnits));
                info.add("Entity Types", String.valueOf(entityTypes));
                info.add("Named Queries", String.valueOf(namedQueries));
                info.render(buffer, y, x, w);
                break;
            case 1:
                if (puNames.isEmpty()) {
                    buffer.setString(x, y, persistenceUnits + " persistence unit(s)", DIM_STYLE);
                } else {
                    buffer.setString(x, y, puNames.size() + " persistence units", DIM_STYLE);
                    puListView.setVisibleRows(h - 2);
                    puListView.setWidth(w);
                    puListView.render(buffer, y + 1, x);
                }
                break;
            case 2:
                if (entityNames.isEmpty()) {
                    buffer.setString(x, y, entityTypes + " entity type(s)", DIM_STYLE);
                } else {
                    buffer.setString(x, y, entityNames.size() + " entities", DIM_STYLE);
                    entityListView.setVisibleRows(h - 2);
                    entityListView.setWidth(w);
                    entityListView.render(buffer, y + 1, x);
                }
                break;
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                activeList().moveDown();
                ctx.requestRedraw();
                return true;
            case 'k':
            case KeyCode.UP:
                activeList().moveUp();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_DOWN:
                activeList().pageDown();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                activeList().pageUp();
                ctx.requestRedraw();
                return true;
            default:
                return false;
        }
    }

    private ListView<String> activeList() {
        return getActiveTab() == 2 ? entityListView : puListView;
    }

    @Override
    protected void onTabChanged(int newTab) {
        // Lists keep their own scroll state
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate";
    }
}
