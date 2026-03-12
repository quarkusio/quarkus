package io.quarkus.webdependency.locator.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for the Web Dependency Locator extension showing web libraries.
 */
public class WebDependencyLocatorShellPage extends BaseExtensionPage {

    private ListView<LibraryInfo> libraryList;
    private List<LibraryInfo> allLibraries = new ArrayList<>();
    private LibraryInfo selectedLibrary;
    private List<String> selectedAssets = new ArrayList<>();

    // No-arg constructor for CDI
    public WebDependencyLocatorShellPage() {
        this.libraryList = new ListView<>(lib -> {
            return lib.name + " v" + lib.version;
        });
    }

    public WebDependencyLocatorShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        allLibraries.clear();
        selectedAssets.clear();
        redraw();

        try {
            String json = getBuildTimeData("quarkus-web-dependency-locator", "webDependencyLibraries");
            loading = false;

            if (json != null && !json.isEmpty()) {
                JsonArray arr = new JsonArray(json);
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject obj = arr.getJsonObject(i);
                    String name = obj.getString("webDependencyName", "");
                    String version = obj.getString("version", "");
                    JsonObject rootAsset = obj.getJsonObject("rootAsset");
                    allLibraries.add(new LibraryInfo(name, version, rootAsset));
                }
            }

            allLibraries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            libraryList.setItems(allLibraries);
            if (!allLibraries.isEmpty()) {
                selectedLibrary = allLibraries.get(0);
                buildAssetList(selectedLibrary);
            }
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load libraries: " + ex.getMessage();
            redraw();
        }
    }

    private void buildAssetList(LibraryInfo library) {
        selectedAssets.clear();
        if (library != null && library.rootAsset != null) {
            collectAssets(library.rootAsset, "");
        }
    }

    private void collectAssets(JsonObject asset, String prefix) {
        if (asset == null) {
            return;
        }

        String name = asset.getString("name", "");
        boolean isFile = asset.getBoolean("fileAsset", false);
        String urlPart = asset.getString("urlPart", "");

        if (isFile) {
            selectedAssets.add(prefix + name + " -> " + urlPart);
        }

        JsonArray children = asset.getJsonArray("children");
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                JsonObject child = children.getJsonObject(i);
                collectAssets(child, prefix + "  ");
            }
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = 3;
        int leftWidth = Math.min(40, width / 3);
        int rightWidth = width - leftWidth - 4;

        // Libraries header
        row++;

        row++;

        if (loading) {
            renderLoading(buffer, row);
        } else if (allLibraries.isEmpty()) {
            buffer.setString(1, row, "No web libraries found", Style.create().gray());
        } else {
            // Render library list on the left
            libraryList.setVisibleRows(height - row - 4);
            libraryList.setWidth(leftWidth);
            libraryList.render(buffer, row, 2);

            // Update selected library based on list selection
            LibraryInfo currentSelected = libraryList.getSelectedItem();
            if (currentSelected != null && currentSelected != selectedLibrary) {
                selectedLibrary = currentSelected;
                buildAssetList(selectedLibrary);
            }

            // Render assets on the right
            int rightCol = leftWidth + 4;
            int assetsRow = 3;

            assetsRow++;

            assetsRow++;

            if (selectedLibrary != null) {
                assetsRow++;

                int maxAssets = Math.min(selectedAssets.size(), height - assetsRow - 4);
                for (int i = 0; i < maxAssets; i++) {
                    String asset = selectedAssets.get(i);
                    if (asset.length() > rightWidth - 2) {
                        asset = asset.substring(0, rightWidth - 5) + "...";
                    }
                    buffer.setString(rightCol, assetsRow, asset, Style.EMPTY);
                    assetsRow++;
                }

                if (selectedAssets.size() > maxAssets) {
                    buffer.setString(rightCol, assetsRow, "+" + (selectedAssets.size() - maxAssets) + " more...",
                            Style.create().gray());
                }
            }

            // Show total count
        }

        if (error != null) {
            renderError(buffer, height - 4);
        }

        renderFooter(buffer, "");
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        if (libraryList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            return;
        }

        if (error != null) {
            return;
        }

        row++;

        if (allLibraries.isEmpty()) {
            return;
        }

        int maxLibs = Math.min(allLibraries.size(), panelHeight - 3);
        for (int i = 0; i < maxLibs; i++) {
            LibraryInfo lib = allLibraries.get(i);
            buffer.setString(startCol, row, truncate(lib.name + " v" + lib.version, panelWidth - 2), Style.EMPTY);
            row++;
        }

        if (allLibraries.size() > maxLibs) {
            buffer.setString(startCol, row, "+" + (allLibraries.size() - maxLibs) + " more...", Style.create().gray());
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        libraryList.setVisibleRows(height - 12);
        libraryList.setWidth(Math.min(40, width / 3));
    }

    private static class LibraryInfo {
        final String name;
        final String version;
        final JsonObject rootAsset;

        LibraryInfo(String name, String version, JsonObject rootAsset) {
            this.name = name != null ? name : "unknown";
            this.version = version != null ? version : "";
            this.rootAsset = rootAsset;
        }
    }
}
