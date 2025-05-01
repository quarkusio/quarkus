package io.quarkus.qute.deployment;

import java.nio.file.Path;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.runtime.QuteConfig;

/**
 * Discovered template.
 * <p>
 * Templates backed by files located in a template root are discovered automatically. Furthermore, extensions can produce this
 * build item in order to provide a template that is not backed by a file.
 *
 * <h2>Warning</h2>
 *
 * Extensions should never <i>consume</i> this build item directly. However, they may consume the
 * {@link EffectiveTemplatePathsBuildItem} instead.
 *
 * @see TemplateRootBuildItem
 */
public final class TemplatePathBuildItem extends MultiBuildItem {

    /**
     * The priority used for templates from the root application archive.
     */
    public static final int ROOT_ARCHIVE_PRIORITY = 30;

    /**
     * The default priority used for templates that are not backed by a file.
     */
    public static final int BUILD_ITEM_PRIORITY = 20;

    /**
     * The priority used for templates from non-root application archives.
     */
    public static final int APP_ARCHIVE_PRIORITY = 10;

    /**
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    static final String TAGS = "tags/";

    private final String path;
    private final String content;
    private final Path fullPath;
    private final String extensionInfo;

    private final int priority;

    /**
     *
     * @param path
     * @param fullPath
     * @param content
     * @deprecated Use the {@link #builder()} instead
     */
    @Deprecated(forRemoval = true, since = "3.13")
    public TemplatePathBuildItem(String path, Path fullPath, String content) {
        this(Objects.requireNonNull(path), Objects.requireNonNull(content), Objects.requireNonNull(fullPath), null,
                BUILD_ITEM_PRIORITY);
    }

    private TemplatePathBuildItem(String path, String content, Path fullPath, String extensionInfo, int priority) {
        this.path = path;
        this.content = content;
        this.fullPath = fullPath;
        this.extensionInfo = extensionInfo;
        this.priority = priority;
    }

    /**
     * The path relative to the template root. The {@code /} is used as a path separator.
     * <p>
     * The path must be unique, i.e. if there are multiple templates with the same path then the template analysis fails during
     * build.
     *
     * @return the path relative to the template root
     */
    public String getPath() {
        return path;
    }

    /**
     * The full path of the template which uses the system-dependent path separator.
     *
     * @return the full path, or {@code null} for templates that are not backed by a file
     */
    public Path getFullPath() {
        return fullPath;
    }

    /**
     *
     * @return the content of the template
     */
    public String getContent() {
        return content;
    }

    /**
     *
     * @return the extension info
     */
    public String getExtensionInfo() {
        return extensionInfo;
    }

    /**
     * Templates with higher priority take precedence when duplicates are found.
     *
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     *
     * @return {@code true} if it represents a user tag, {@code false} otherwise
     */
    public boolean isTag() {
        return path.startsWith(TAGS);
    }

    /**
     *
     * @return {@code true} if it does not represent a tag, {@code false} otherwise
     */
    public boolean isRegular() {
        return !isTag();
    }

    /**
     *
     * @return {@code true} if it's backed by a file
     */
    public boolean isFileBased() {
        return fullPath != null;
    }

    public String getSourceInfo() {
        return (isFileBased() ? getFullPath().toString() : extensionInfo) + " [" + getPriority() + "]";
    }

    public static class Builder {

        private String path;
        private String content;
        private Path fullPath;
        private String extensionInfo;
        private int priority = BUILD_ITEM_PRIORITY;

        /**
         * Set the path relative to the template root. The {@code /} is used as a path separator.
         * <p>
         * The path must be unique, i.e. if there are multiple templates with the same path then the template analysis fails
         * during build.
         *
         * @param path
         * @return self
         */
        public Builder path(String path) {
            this.path = Objects.requireNonNull(path);
            return this;
        }

        /**
         * Set the content of the template.
         *
         * @param content
         * @return self
         */
        public Builder content(String content) {
            this.content = Objects.requireNonNull(content);
            return this;
        }

        /**
         * Set the full path of the template for templates that are backed by a file.
         *
         * @param fullPath
         * @return self
         */
        public Builder fullPath(Path fullPath) {
            this.fullPath = Objects.requireNonNull(fullPath);
            return this;
        }

        /**
         * Set the extension info for templates that are not backed by a file.
         *
         * @param info
         * @return self
         */
        public Builder extensionInfo(String info) {
            this.extensionInfo = info;
            return this;
        }

        /**
         * Set the priority of the template.
         *
         * @param priority
         * @return self
         * @see QuteConfig#duplicitTemplatesStrategy()
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public TemplatePathBuildItem build() {
            if (fullPath == null && extensionInfo == null) {
                throw new IllegalStateException("Templates that are not backed by a file must provide extension info");
            }
            return new TemplatePathBuildItem(path, content, fullPath, extensionInfo, priority);
        }

    }

}
