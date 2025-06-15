package io.quarkus.devui.spi.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableDataPageBuilder extends BuildTimeDataPageBuilder<TableDataPageBuilder> {
    private static final String QWC_DATA_TABLE_PAGE_JS = "qwc-data-table-page.js";
    private static final String COLS = "cols";
    private static final String COMMA = ",";

    protected TableDataPageBuilder(String title) {
        super(title);
        super.componentLink = QWC_DATA_TABLE_PAGE_JS;
    }

    public TableDataPageBuilder showColumn(String path) {
        List<String> headerPaths = new ArrayList<>();
        if (super.metadata.containsKey(COLS)) {
            String csl = super.metadata.get(COLS);
            headerPaths = new ArrayList<>(Arrays.asList(csl.split(COMMA)));
        }
        headerPaths.add(path);
        String csl = String.join(COMMA, headerPaths);
        super.metadata(COLS, csl);
        return this;
    }
}
