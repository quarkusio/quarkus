package io.quarkus.devui.spi.page;

/**
 * Define a card in Dev UI. This is only used when an extension wants to supply a custom card (i.e. the default with
 * links is not sufficient)
 */
public class Card {

    private final String componentName; // This is the name (tagName) of the component
    private final String componentLink; // This is a link to the component, excluding namespace
    private String namespace; // The namespace (a.k.a extension path)

    public Card(String componentLink) {
        if (componentLink.endsWith(DOT_JS)) {
            this.componentLink = componentLink;
        } else {
            this.componentLink = componentLink + DOT_JS;
        }

        this.componentName = this.componentLink.substring(0, this.componentLink.length() - 3);
    }

    public String getComponentRef() {
        if (this.namespace != null) {
            return DOT + SLASH + DOT + DOT + SLASH + this.namespace + SLASH + this.componentLink;
        }
        // TODO: Create a not found component to display here ?
        throw new RuntimeException("Could not find component reference");
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getComponentLink() {
        return componentLink;
    }

    public String getComponentName() {
        return componentName;
    }

    @Override
    public String toString() {
        return "Card {\n\tnamespace=" + namespace + ", \n\tcomponentLink=" + componentLink + "\n}";
    }

    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String DOT_JS = DOT + "js";
}
