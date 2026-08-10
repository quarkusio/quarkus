package io.quarkus.devshell.deployment.tui.pages;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;

public interface ExtensionPage extends Screen {

    String getNamespace();

    void loadData();

    void renderPanel(Rect area, Buffer buffer);

    boolean handlePanelKey(int[] keys);

    void initPanel(AppContext ctx);

    void reset();
}
