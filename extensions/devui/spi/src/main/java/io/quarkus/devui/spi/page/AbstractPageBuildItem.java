package io.quarkus.devui.spi.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Any of card, menu or footer pages
 */
public abstract class AbstractPageBuildItem extends AbstractDevUIBuildItem {

    protected final Map<String, Object> buildTimeData;
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
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public AbstractPageBuildItem(String customIdentifier) {
        super(customIdentifier);
        this.buildTimeData = new HashMap<>();
        this.pageBuilders = new ArrayList<>();
    }

    public AbstractPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier);
        this.buildTimeData = new HashMap<>();
        this.pageBuilders = Arrays.asList(pageBuilder);
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
        this.buildTimeData.put(fieldName, fieldData);
    }

    public Map<String, Object> getBuildTimeData() {
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
