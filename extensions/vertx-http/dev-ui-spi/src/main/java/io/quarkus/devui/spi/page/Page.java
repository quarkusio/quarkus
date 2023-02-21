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

    private final boolean embed; // if the component is embeded in the page. true in all cases except maybe external pages
    private final boolean internalComponent; // True f this component is provided by dev-ui (usually provided by the extension)

    private String namespace = null; // The namespace can be the extension path or, if internal, qwc

    protected Page(String icon,
            String title,
            String staticLabel,
            String dynamicLabel,
            String streamingLabel,
            String componentName,
            String componentLink,
            Map<String, String> metadata,
            boolean embed,
            boolean internalComponent,
            String namespace) {

        this.icon = icon;
        this.title = title;
        this.staticLabel = staticLabel;
        this.dynamicLabel = dynamicLabel;
        this.streamingLabel = streamingLabel;
        this.componentName = componentName;
        this.componentLink = componentLink;
        this.metadata = metadata;
        this.embed = embed;
        this.internalComponent = internalComponent;
        this.namespace = namespace;
    }

    public String getId() {
        String id = this.title.toLowerCase().replaceAll(SPACE, DASH);
        if (this.namespace != null) {
            id = this.namespace + SLASH + id;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Page {\n\ticon=" + icon
                + ", \n\ttitle=" + title
                + ", \n\tstaticLabel=" + staticLabel
                + ", \n\tdynamicLabel=" + dynamicLabel
                + ", \n\tstreamingLabel=" + streamingLabel
                + ", \n\tnamespace=" + namespace
                + ", \n\tcomponentName=" + componentName
                + ", \n\tcomponentLink=" + componentLink
                + ", \n\tembed=" + embed + "\n}";
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
