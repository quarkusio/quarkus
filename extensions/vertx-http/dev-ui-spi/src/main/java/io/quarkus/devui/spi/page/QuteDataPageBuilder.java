package io.quarkus.devui.spi.page;

public class QuteDataPageBuilder extends PageBuilder<QuteDataPageBuilder> {
    private static final String DOT_HTML = ".html";
    private static final String QWC_DATA_QUTE_PAGE_JS = "qwc-data-qute-page.js";
    private String templateLink;

    protected QuteDataPageBuilder(String title) {
        super();
        super.title = title;
        super.internalComponent = true;// As external page runs on "internal" namespace
        super.componentLink = QWC_DATA_QUTE_PAGE_JS;
    }

    public QuteDataPageBuilder templateLink(String templateLink) {
        if (templateLink == null || templateLink.isEmpty() || !templateLink.endsWith(DOT_HTML)) {
            throw new RuntimeException(
                    "Invalid template link [" + templateLink + "] - Expeting a link that ends with .html");
        }

        this.templateLink = templateLink;
        return this;
    }

    @Override
    public Page build() {

        super.metadata("templatePath", getTemplatePath());
        return super.build();
    }

    public String getTemplatePath() {
        return "/dev-ui/" + this.templateLink;
    }

}
