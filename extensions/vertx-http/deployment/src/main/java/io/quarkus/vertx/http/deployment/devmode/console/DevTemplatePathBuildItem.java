package io.quarkus.vertx.http.deployment.devmode.console;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a dev template path.
 */
public final class DevTemplatePathBuildItem extends MultiBuildItem {

    static final String TAGS = "tags/";

    private final String path;
    private final String contents;

    public DevTemplatePathBuildItem(String path, String contents) {
        this.path = path;
        this.contents = contents;
    }

    /**
     * Uses the {@code /} path separator.
     * 
     * @return the path relative to the template root
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the template contents
     * 
     * @return the template contents
     */
    public String getContents() {
        return contents;
    }

    /**
     * 
     * @return {@code true} if it represents a user tag, {@code false} otherwise
     */
    public boolean isTag() {
        return path.startsWith(TAGS);
    }

    public boolean isRegular() {
        return !isTag();
    }

    public String getTagName() {
        int end = path.lastIndexOf('.');
        if (end == -1) {
            end = path.length();
        }
        return path.substring(TAGS.length(), end);
    }

}
