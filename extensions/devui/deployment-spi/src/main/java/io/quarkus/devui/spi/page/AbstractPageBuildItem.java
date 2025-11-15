package io.quarkus.devui.spi.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeData;

/**
 * Any of card, menu or footer pages
 */
public abstract class AbstractPageBuildItem extends AbstractDevUIBuildItem {

    protected final Map<String, BuildTimeData> buildTimeData;
    protected final List<PageBuilder> pageBuilders;
    protected String headlessComponentLink = null;

    public AbstractPageBuildItem() {
        super();
        this.buildTimeData = new HashMap<>();
        this.pageBuilders = new ArrayList<>();
    }

    public AbstractPageBuildItem(PageBuilder... pageBuilder) {
        super();
        this.buildTimeData = new HashMap<>();
        this.pageBuilders = new ArrayList<>(Arrays.asList(pageBuilder));
    }

    public AbstractPageBuildItem(String customIdentifier) {
        super(customIdentifier);
        this.buildTimeData = new HashMap<>();
        this.pageBuilders = new ArrayList<>();
    }

    public AbstractPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier);
        this.buildTimeData = new HashMap<>();
        this.pageBuilders = new ArrayList<>(Arrays.asList(pageBuilder));
    }

    public void addPage(PageBuilder page) {
        this.pageBuilders.add(page);
    }

    public List<PageBuilder> getPages() {
        return this.pageBuilders;
    }

    public boolean hasPages() {
        return !pageBuilders.isEmpty();
    }

    public void addBuildTimeData(String fieldName, Object fieldData) {
        this.addBuildTimeData(fieldName, fieldData, null);
    }

    public void addBuildTimeData(String fieldName, Object fieldData, String description) {
        this.buildTimeData.put(fieldName, new BuildTimeData(fieldData, description));
    }

    public void addBuildTimeData(String fieldName, Object fieldData, String description, boolean mcpEnabledByDefault) {
        this.buildTimeData.put(fieldName, new BuildTimeData(fieldData, description, mcpEnabledByDefault));
    }

    public Map<String, BuildTimeData> getBuildTimeData() {
        return this.buildTimeData;
    }

    public boolean hasBuildTimeData() {
        return this.buildTimeData != null && !this.buildTimeData.isEmpty();
    }

    public void setHeadlessComponentLink(String headlessComponentLink) {
        this.headlessComponentLink = headlessComponentLink;
    }

    public String getHeadlessComponentLink() {
        return this.headlessComponentLink;
    }
}
