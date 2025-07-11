package io.quarkus.devui.spi.page;

public class WebComponentPageBuilder extends PageBuilder<WebComponentPageBuilder> {

    protected WebComponentPageBuilder() {
        super();
    }

    protected WebComponentPageBuilder(String icon, String color, String tooltip) {
        super(icon, color, tooltip, true);
    }

    public WebComponentPageBuilder componentName(String componentName) {
        if (componentName == null || componentName.isEmpty()) {
            throw new RuntimeException("Invalid component [" + componentName + "]");
        }

        super.componentName = componentName;
        return this;
    }

    public WebComponentPageBuilder componentLink(String componentLink) {
        if (componentLink == null || componentLink.isEmpty() || !componentLink.endsWith(DOT_JS)) {
            throw new RuntimeException(
                    "Invalid component link [" + componentLink + "] - Expeting a link that ends with .js");
        }

        super.componentLink = componentLink;
        return this;
    }
}