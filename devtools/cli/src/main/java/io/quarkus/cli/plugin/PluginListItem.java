package io.quarkus.cli.plugin;

public class PluginListItem {

    private final boolean installed;
    private final Plugin plugin;

    public PluginListItem(boolean installed, Plugin plugin) {
        this.installed = installed;
        this.plugin = plugin;
    }

    public boolean isInstalled() {
        return installed;
    }

    public String getSymbol() {
        return installed ? "*" : " ";
    }

    public String getName() {
        return plugin.getName();
    }

    public String getType() {
        return plugin.getType().name();
    }

    public String getScope() {
        return plugin.isInUserCatalog() ? "user" : "project";
    }

    public String getLocation() {
        return plugin.getLocation().orElse("");
    }

    public String getDescription() {
        return plugin.getDescription().orElse("");
    }

    public String getCommand() {
        switch (plugin.getType()) {
            case jar:
            case maven:
                return "jbang " + plugin.getLocation().orElse("<unknown>");
            case jbang:
                return "jbang " + plugin.getLocation().orElse(plugin.getName());
            case executable:
                return plugin.getLocation().orElse("<unknown>");
            default:
                return "";
        }
    }

    public String[] getFields() {
        return getFields(false);
    }

    public String[] getFields(boolean withCommand) {
        return withCommand
                ? new String[] { getSymbol(), getName(), getType(), getScope(), getLocation(), getDescription(),
                        getCommand() }
                : new String[] { getSymbol(), getName(), getType(), getScope(), getLocation(), getDescription() };
    }
}
