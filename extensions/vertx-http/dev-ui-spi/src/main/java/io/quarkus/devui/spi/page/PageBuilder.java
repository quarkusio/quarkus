package io.quarkus.devui.spi.page;

import java.util.HashMap;
import java.util.Map;

public abstract class PageBuilder<T> {
    protected static final String EMPTY = "";
    protected static final String SPACE = " ";
    protected static final String DASH = "-";
    protected static final String DOT = ".";
    protected static final String JS = "js";
    protected static final String QWC_DASH = "qwc-";
    protected static final String DOT_JS = DOT + JS;

    protected String icon = "font-awesome-solid:arrow-right";
    protected String title = null;
    protected String staticLabel = null;
    protected String dynamicLabel = null;
    protected String streamingLabel = null;
    protected String componentName;
    protected String componentLink;
    protected Map<String, String> metadata = new HashMap<>();
    protected boolean embed = true; // default
    protected boolean includeInSubMenu = true; // default
    protected boolean internalComponent = false; // default
    protected String namespace = null;
    protected String namespaceLabel = null;
    protected String extensionId = null;
    protected Class preprocessor = null;

    @SuppressWarnings("unchecked")
    public T icon(String icon) {
        this.icon = icon;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T title(String title) {
        this.title = title;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T staticLabel(String staticLabel) {
        this.staticLabel = staticLabel;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T dynamicLabelJsonRPCMethodName(String methodName) {
        this.dynamicLabel = methodName;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T streamingLabelJsonRPCMethodName(String methodName) {
        this.streamingLabel = methodName;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T metadata(String key, String value) {
        this.metadata.put(key, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T namespace(String namespace) {
        if (this.namespace == null) {
            this.namespace = namespace;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T internal() {
        return this.internal(null);
    }

    @SuppressWarnings("unchecked")
    public T internal(String namespaceLabel) {
        this.internalComponent = true;
        this.namespaceLabel = namespaceLabel;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T extension(String extension) {
        this.extensionId = extension.toLowerCase().replaceAll(SPACE, DASH);
        this.metadata.put("extensionName", extension);
        this.metadata.put("extensionId", extensionId); // TODO: Remove ?
        return (T) this;
    }

    public Page build() {
        if (this.componentName == null && this.componentLink == null && this.title == null) {
            throw new RuntimeException(
                    "Not enough information to build the page. Set at least one of componentLink and/or componentName and/or title");
        }

        // Guess the component link from the component name or title
        if (this.componentLink == null) {
            if (this.componentName != null) {
                this.componentLink = this.componentName + DOT_JS;
            } else if (this.title != null) {
                this.componentLink = QWC_DASH + this.title.toLowerCase().replaceAll(SPACE, DASH) + DOT_JS;
            }
        }

        // Guess the component name from the componentlink or title
        if (this.componentName == null) {
            this.componentName = this.componentLink.substring(0, this.componentLink.lastIndexOf(DOT)); // Remove the file extension (.js)
        }

        // Guess the title
        if (this.title == null) {
            String n = this.componentName.replaceAll(QWC_DASH, EMPTY); // Remove the qwc-
            n = n.substring(n.indexOf(DASH) + 1); // Remove the namespace-
            n = n.replaceAll(DASH, SPACE);
            this.title = n.substring(0, 1).toUpperCase() + n.substring(1); // Capitalize first letter
        }

        Page page = new Page(icon,
                title,
                staticLabel,
                dynamicLabel,
                streamingLabel,
                componentName,
                componentLink,
                metadata,
                embed,
                includeInSubMenu,
                internalComponent,
                namespace,
                namespaceLabel,
                extensionId);

        return page;
    }
}
