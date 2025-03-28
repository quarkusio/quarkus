package io.quarkus.devui.spi.workspace;

/**
 * Types of Display for the UI
 */
public enum Display {
    nothing, // Nothing will be displayed
    dialog, // Content will be displayed in a dialog popup
    replace, // Content will replace the original (input) content
    split, // Content will display in a split screen
    notification // Content will in a notification
}
