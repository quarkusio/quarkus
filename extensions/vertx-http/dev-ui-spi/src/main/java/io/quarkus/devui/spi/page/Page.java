package io.quarkus.devui.spi.page;

import java.util.Map;

/**
 * Define a page in Dev UI.
 * This is not a full web page, but rather the section in the middle where extensions can display data.
 * All pages (fragments) are rendered using Web components, but different builders exist to make it easy to define a page
 *
 * Navigation to this page is also defined here.
 */
public class Page {
    private final String icon; // Any font awesome icon
    private final String title; // This is the display name and link title for the page
    private final String staticLabel; // This is optional extra info that might be displayed next to the link
    private final String dynamicLabel; // This is optional extra info that might be displayed next to the link. This will override above static label. This expects a jsonRPC method name
    private final String streamingLabel; // This is optional extra info that might be displayed next to the link. This will override above dynamic label. This expects a jsonRPC Multi method name

    private final String componentName; // This is name of the component
    private final String componentLink; // This is a link to the component, excluding namespace
    private final Map<String, String> metadata; // Key value Metadata

    private final boolean embed; // if the component is embedded in the page. true in all cases except maybe external pages
    private final boolean includeInSubMenu; // if this link should be added to the submenu. true in all cases except maybe external pages
    private final boolean internalComponent; // True if this component is provided by dev-ui (usually provided by the extension)

    private String namespace = null; // The namespace can be the extension path or, if internal, qwc
    private String namespaceLabel = null; // When more than one page belongs to the same namespace, we use the namespace as a title sometimes
    private String extensionId = null; // If this originates from an extension, then id. For internal this will be null;

    protected Page(String icon,
            String title,
            String staticLabel,
            String dynamicLabel,
            String streamingLabel,
            String componentName,
            String componentLink,
            Map<String, String> metadata,
            boolean embed,
            boolean includeInSubMenu,
            boolean internalComponent,
            String namespace,
            String namespaceLabel,
            String extensionId) {

        this.icon = icon;
        this.title = title;
        this.staticLabel = staticLabel;
        this.dynamicLabel = dynamicLabel;
        this.streamingLabel = streamingLabel;
        this.componentName = componentName;
        this.componentLink = componentLink;
        this.metadata = metadata;
        this.embed = embed;
        this.includeInSubMenu = includeInSubMenu;
        this.internalComponent = internalComponent;
        this.namespace = namespace;
        this.namespaceLabel = namespaceLabel;
        this.extensionId = extensionId;
    }

    public String getId() {
        String id = this.title.toLowerCase().replaceAll(SPACE, DASH);
        if (!this.isInternal() && this.namespace != null) {
            // This is extension pages in Dev UI
            id = this.namespace.toLowerCase() + SLASH + id;
        } else if (this.isInternal() && this.namespace != null) {
            // This is internal pages in Dev UI
            String d = "devui-" + id;
            if (d.equals(this.namespace)) {
                return id;
            } else {
                int i = this.namespace.indexOf(DASH) + 1;
                String stripDevui = this.namespace.substring(i);
                return stripDevui + DASH + id;
            }
        }
        return id;
    }

    public String getComponentRef() {
        if (internalComponent) {
            return DOT + SLASH + DOT + DOT + SLASH + "qwc" + SLASH + this.componentLink;
        } else if (this.namespace != null) {
            return DOT + SLASH + DOT + DOT + SLASH + this.namespace + SLASH + this.componentLink;
        }
        // TODO: Create a not found component to display here ?
        throw new RuntimeException("Could not find component reference");
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getNamespaceLabel() {
        return this.namespaceLabel;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getStaticLabel() {
        return staticLabel;
    }

    public String getDynamicLabel() {
        return dynamicLabel;
    }

    public String getStreamingLabel() {
        return streamingLabel;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentLink() {
        return componentLink;
    }

    public boolean isEmbed() {
        return embed;
    }

    public boolean isIncludeInSubMenu() {
        return includeInSubMenu;
    }

    public boolean isInternal() {
        return this.internalComponent && this.extensionId == null;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Page {\n\tid=" + getId()
                + ", \n\ticon=" + icon
                + ", \n\ttitle=" + title
                + ", \n\tstaticLabel=" + staticLabel
                + ", \n\tdynamicLabel=" + dynamicLabel
                + ", \n\tstreamingLabel=" + streamingLabel
                + ", \n\tnamespace=" + namespace
                + ", \n\tnamespaceLabel=" + namespaceLabel
                + ", \n\tcomponentName=" + componentName
                + ", \n\tcomponentLink=" + componentLink
                + ", \n\tembed=" + embed
                + ", \n\tincludeInSubMenu=" + includeInSubMenu + "\n}";
    }

    /**
     * Here you provide the Web Component that should be rendered. You have full control over the page.
     * You can use build time data if you made it available
     */
    public static WebComponentPageBuilder webComponentPageBuilder() {
        return new WebComponentPageBuilder();
    }

    /**
     * Here you provide a url to an external resource. When code/markup, if can be displayed in a code view, when HTML it can
     * render the HTML
     */
    public static ExternalPageBuilder externalPageBuilder(String name) {
        return new ExternalPageBuilder(name);
    }

    /**
     * Here you provide the data that should be rendered in raw json format
     */
    public static RawDataPageBuilder rawDataPageBuilder(String name) {
        return new RawDataPageBuilder(name);
    }

    /**
     * Here you can render the data with a qute template
     */
    public static QuteDataPageBuilder quteDataPageBuilder(String name) {
        return new QuteDataPageBuilder(name);
    }

    /**
     * Here you provide the data that should be rendered in a table
     */
    public static TableDataPageBuilder tableDataPageBuilder(String name) {
        return new TableDataPageBuilder(name);
    }

    private static final String SPACE = " ";
    private static final String DASH = "-";
    private static final String SLASH = "/";
    private static final String DOT = ".";

}
