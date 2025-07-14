package io.quarkus.devui.spi.page;

import org.jboss.logging.Logger;

public class ExternalPageBuilder extends PageBuilder<ExternalPageBuilder> {
    private static final Logger log = Logger.getLogger(ExternalPageBuilder.class);

    private static final String QWC_EXTERNAL_PAGE_JS = "qwc-external-page.js";
    private static final String EXTERNAL_URL = "externalUrl";
    private static final String DYNAMIC_URL = "dynamicUrlMethodName";
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
        return url(url, null);
    }

    public ExternalPageBuilder url(String url, String externalLink) {
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("Invalid external URL, can not be empty");
        }
        super.metadata.put(EXTERNAL_URL, url);
        if (externalLink != null) {
            return staticLabel("<a style='color: var(--lumo-contrast-80pct);' href='" + externalLink
                    + "' target='_blank'><vaadin-icon class='icon' icon='font-awesome-solid:up-right-from-square'></vaadin-icon></a>");
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public ExternalPageBuilder dynamicUrlJsonRPCMethodName(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            throw new RuntimeException("Invalid dynamic URL Method name, can not be empty");
        }
        super.metadata.put(DYNAMIC_URL, methodName);
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
            log.warn("MimeType already set to " + super.metadata.get(MIME_TYPE) + ", overriding with new value");
        }
        super.metadata.put(MIME_TYPE, mimeType);
        return this;
    }

    public ExternalPageBuilder doNotEmbed() {
        return doNotEmbed(false);
    }

    public ExternalPageBuilder doNotEmbed(boolean includeInMenu) {
        super.embed = false;
        super.includeInMenu = includeInMenu;
        return this;
    }

}