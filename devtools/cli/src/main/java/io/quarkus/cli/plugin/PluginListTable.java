package io.quarkus.cli.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class PluginListTable {

    private static final String INSTALLED = " ";
    private static final String NAME = "Name";
    private static final String TYPE = "Type";
    private static final String SCOPE = "Scope";
    private static final String LOCATION = "Location";
    private static final String DESCRIPTION = "Description";
    private static final String COMMAND = "Command";

    private static final String NEWLINE = "\n";

    private List<PluginListItem> items;
    private boolean withCommand;
    private boolean withDiff;

    public PluginListTable(Collection<PluginListItem> items) {
        this(items, false, false);
    }

    public PluginListTable(Collection<PluginListItem> items, boolean withCommand) {
        this(items, withCommand, false);
    }

    public PluginListTable(Collection<PluginListItem> items, boolean withCommand, boolean withDiff) {
        this.items = new ArrayList<>(items);
        this.withCommand = withCommand;
        this.withDiff = withDiff;
    }

    public PluginListTable() {
    }

    public String getContent() {
        return getContent(items, withCommand, withDiff);
    }

    // Utils
    private static String[] getLabels() {
        return getLabels(false);
    }

    private static String[] getLabels(boolean withCommand) {
        if (withCommand) {
            return new String[] { INSTALLED, NAME, TYPE, SCOPE, LOCATION, DESCRIPTION, COMMAND };
        } else {
            return new String[] { INSTALLED, NAME, TYPE, SCOPE, LOCATION, DESCRIPTION };
        }
    }

    private static String getHeader(String format, Collection<PluginListItem> items, boolean withCommand) {
        return String.format(format, getLabels(withCommand));
    }

    private static String getBody(String format, Collection<PluginListItem> items, boolean withCommand,
            boolean withDiff) {
        StringBuilder sb = new StringBuilder();
        for (PluginListItem item : items) {
            sb.append(String.format(format, fieldsWithDiff(item.getFields(withCommand), withDiff)));
            sb.append(NEWLINE);
        }
        return sb.toString();
    }

    public static String getContent(Collection<PluginListItem> items, boolean wtihCommand, boolean withDiff) {
        String format = getFormat(items, wtihCommand);
        return getContent(format, items, wtihCommand, withDiff);
    }

    public static String getContent(String format, Collection<PluginListItem> items, boolean wtihCommand,
            boolean withDiff) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeader(format, items, wtihCommand));
        sb.append(NEWLINE);
        sb.append(getBody(format, items, wtihCommand, withDiff));
        return sb.toString();
    }

    private static String getFormat(Collection<PluginListItem> items, boolean withCommand) {
        StringBuilder sb = new StringBuilder();
        sb.append(" %-1s ");

        int maxNameLength = Stream.concat(Stream.of(NAME), items.stream().map(PluginListItem::getName))
                .filter(Objects::nonNull).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        sb.append(" %-" + maxNameLength + "s ");
        sb.append("\t");

        int maxTypeLength = Stream.concat(Stream.of(TYPE), items.stream().map(PluginListItem::getType))
                .filter(Objects::nonNull).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        sb.append(" %-" + maxTypeLength + "s ");
        sb.append("\t");

        int maxScopeLength = Stream.concat(Stream.of(SCOPE), items.stream().map(PluginListItem::getScope))
                .filter(Objects::nonNull).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        sb.append(" %-" + maxScopeLength + "s ");
        sb.append("\t");

        int maxLocationLength = Stream.concat(Stream.of(LOCATION), items.stream().map(PluginListItem::getLocation))
                .filter(Objects::nonNull).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        sb.append(" %-" + maxLocationLength + "s ");
        sb.append("\t");

        int maxDescriptionLength = Stream
                .concat(Stream.of(DESCRIPTION), items.stream().map(PluginListItem::getDescription))
                .filter(Objects::nonNull).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        sb.append(" %-" + maxDescriptionLength + "s ");
        sb.append("\t");

        if (withCommand) {
            int maxCommandLength = Stream.concat(Stream.of(COMMAND), items.stream().map(PluginListItem::getCommand))
                    .filter(Objects::nonNull).map(String::length).max(Comparator.naturalOrder()).orElse(0);
            sb.append(" %-" + maxCommandLength + "s ");
        }
        return sb.toString();
    }

    private static String[] fieldsWithDiff(String[] fields, boolean showDiff) {
        if (!showDiff) {
            return fields;
        }
        // Map '*'' -> '+'' and ' ' -> '-'
        fields[0] = fields[0].replace("*", "+").replace(" ", "-");
        return fields;
    }
}
