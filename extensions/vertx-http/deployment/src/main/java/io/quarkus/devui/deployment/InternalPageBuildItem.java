package io.quarkus.devui.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;

/**
 * Used internally to define some of our own pages
 */
public final class InternalPageBuildItem extends MultiBuildItem {

    private final String namespaceLabel;
    private final int position;
    private final List<Page> pages = new ArrayList<>();
    private final Map<String, Object> buildTimeData = new HashMap<>();

    public InternalPageBuildItem(String namespaceLabel, int position) {
        this.namespaceLabel = namespaceLabel;
        this.position = position;
    }

    public void addPage(PageBuilder page) {
        page = (PageBuilder) page.internal(this.namespaceLabel);
        this.pages.add(page.build());
    }

    public void addBuildTimeData(String key, Object value) {
        this.buildTimeData.put(key, value);
    }

    public List<Page> getPages() {
        return pages;
    }

    public int getPosition() {
        return position;
    }

    public String getNamespaceLabel() {
        return namespaceLabel;
    }

    public Map<String, Object> getBuildTimeData() {
        return buildTimeData;
    }
}