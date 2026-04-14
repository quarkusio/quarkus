package io.quarkus.cache.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.cache.runtime.dev.ui.CacheJsonRPCService;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for the Cache extension showing cache statistics.
 */
public class CacheShellPage extends BaseExtensionPage {

    private ListView<CacheInfo> cacheList;
    private List<CacheInfo> allCaches = new ArrayList<>();

    @Inject
    CacheJsonRPCService cacheService;

    // No-arg constructor for CDI
    public CacheShellPage() {
        this.cacheList = new ListView<>(cache -> {
            String sizeStr = cache.size >= 0 ? String.valueOf(cache.size) : "?";
            return cache.name + " (" + sizeStr + " entries)";
        });
    }

    public CacheShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        allCaches.clear();
        redraw();

        try {
            JsonArray array = cacheService.getAll();
            loading = false;
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.getJsonObject(i);
                    String name = obj.getString("name");
                    if (name != null) {
                        long size = obj.getLong("size", -1L);
                        allCaches.add(new CacheInfo(name, size));
                    }
                }
            }
            allCaches.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            cacheList.setItems(allCaches);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load caches: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = 3;

        row++;

        row++;

        if (loading) {
            renderLoading(buffer, row);
        } else if (cacheList.isEmpty()) {
            buffer.setString(1, row, "No caches found", Style.create().gray());
        } else {
            cacheList.setVisibleRows(height - row - 6);
            cacheList.setWidth(width - 4);
            cacheList.render(buffer, row, 2);

            // Show selected cache details
            CacheInfo selected = cacheList.getSelectedItem();
            if (selected != null) {
                int detailRow = height - 5;
                KeyValuePanel detail = new KeyValuePanel();
                detail.add("Cache", selected.name);
                detail.add("Size", selected.size >= 0 ? String.valueOf(selected.size) : "unknown");
                detail.render(buffer, detailRow, 2, width - 4);
            }

        }

        if (error != null) {
            renderError(buffer, height - 4);
        }

        renderFooter(buffer, "[C] Clear cache  [A] Clear all caches");
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        if (cacheList.handleKey(key)) {
            redraw();
            return true;
        }

        switch (key) {
            case 'c':
            case 'C':
                clearSelectedCache();
                return true;
            case 'a':
            case 'A':
                clearAllCaches();
                return true;
            default:
                return super.handleKey(key);
        }
    }

    private void clearSelectedCache() {
        CacheInfo selected = cacheList.getSelectedItem();
        if (selected == null)
            return;

        loading = true;
        ctx.setStatus("Clearing cache: " + selected.name + "...");
        redraw();

        try {
            cacheService.clear(selected.name).await().indefinitely();
            loading = false;
            ctx.setStatus("Cache cleared: " + selected.name);
            loadData();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to clear cache: " + ex.getMessage();
            redraw();
        }
    }

    private void clearAllCaches() {
        loading = true;
        ctx.setStatus("Clearing all caches...");
        redraw();

        try {
            // Clear all caches by iterating
            for (CacheInfo cache : allCaches) {
                cacheService.clear(cache.name).await().indefinitely();
            }
            loading = false;
            ctx.setStatus("All caches cleared");
            loadData();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to clear caches: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            renderPanelLoading(buffer, row, startCol);
            return;
        }

        if (error != null) {
            renderPanelError(buffer, row, startCol, panelWidth);
            return;
        }

        row++;

        if (allCaches.isEmpty()) {
            return;
        }

        int maxCaches = Math.min(allCaches.size(), panelHeight - 3);
        for (int i = 0; i < maxCaches; i++) {
            CacheInfo cache = allCaches.get(i);
            String sizeStr = cache.size >= 0 ? String.valueOf(cache.size) : "?";
            buffer.setString(startCol, row, truncate(cache.name + " (" + sizeStr + ")", panelWidth - 2), Style.EMPTY);
            row++;
        }

        if (allCaches.size() > maxCaches) {
            buffer.setString(startCol, row, "+" + (allCaches.size() - maxCaches) + " more...", Style.create().gray());
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        cacheList.setVisibleRows(height - 12);
        cacheList.setWidth(width - 4);
    }

    private static class CacheInfo {
        final String name;
        final long size;

        CacheInfo(String name, long size) {
            this.name = name != null ? name : "unknown";
            this.size = size;
        }
    }
}
