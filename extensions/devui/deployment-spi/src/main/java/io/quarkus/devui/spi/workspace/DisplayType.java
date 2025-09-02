package io.quarkus.devui.spi.workspace;

/**
 * Supported content types
 */
public enum DisplayType {
    raw, // This be used as is (text)
    code, // This will be rendered in a code editor
    markdown, // This will display interperated markdown
    html, // This will display interperated html
    image, // This will display an image
}
