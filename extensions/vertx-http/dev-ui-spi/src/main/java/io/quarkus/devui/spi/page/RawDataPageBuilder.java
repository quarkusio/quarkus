package io.quarkus.devui.spi.page;

public class RawDataPageBuilder extends BuildTimeDataPageBuilder<RawDataPageBuilder> {
    private static final String QWC_DATA_RAW_PAGE_JS = "qwc-data-raw-page.js";

    protected RawDataPageBuilder(String title) {
        super(title);
        super.componentLink = QWC_DATA_RAW_PAGE_JS;
    }
}
