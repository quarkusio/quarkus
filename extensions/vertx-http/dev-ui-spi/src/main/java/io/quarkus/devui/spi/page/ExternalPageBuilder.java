package io.quarkus.devui.spi.page;

import io.quarkus.logging.Log;

public class ExternalPageBuilder extends PageBuilder<ExternalPageBuilder> {
    private static final String QWC_EXTERNAL_PAGE_JS = "qwc-external-page.js";
    private static final String EXTERNAL_URL = "externalUrl";
    private static final String MIME_TYPE = "mimeType";

    public static final String MIME_TYPE_HTML = "text/html";
    public static final String MIME_TYPE_JSON = "application/json";
    public static final String MIME_TYPE_YAML = "application/yaml";
    public static final String MIME_TYPE_PDF = "application/pdf";

    protected ExternalPageBuilder(String title) {
        super();
        super.title = title;
        super.componentLink = QWC_EXTERNAL_PAGE_JS;
        super.internalComponent = true;// As external page runs on "internal" namespace
    }

    public ExternalPageBuilder url(String url) {
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("Invalid external URL, can not be empty");
        }
        super.metadata.put(EXTERNAL_URL, url);
        return this;
    }

    public ExternalPageBuilder isHtmlContent() {
        return mimeType(MIME_TYPE_HTML);
    }

    public ExternalPageBuilder isJsonContent() {
        return mimeType(MIME_TYPE_JSON);
    }

    public ExternalPageBuilder isYamlContent() {
        return mimeType(MIME_TYPE_YAML);
    }

    public ExternalPageBuilder isPdfContent() {
        return mimeType(MIME_TYPE_PDF);
    }

    public ExternalPageBuilder mimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            throw new RuntimeException("Invalid mimeType, can not be empty");
        }
        if (super.metadata.containsKey(MIME_TYPE)) {
            Log.warn("MimeType already set to " + super.metadata.get(MIME_TYPE) + ", overriding with new value");
        }
        super.metadata.put(MIME_TYPE, mimeType);
        return this;
    }

    public ExternalPageBuilder doNotEmbed() {
        super.embed = false;
        return this;
    }
}