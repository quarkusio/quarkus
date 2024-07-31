package io.quarkus.annotation.processor.documentation.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * At this stage, a config root is actually a prefix: we merged all the config roots with the same prefix.
 * <p>
 * Thus the phase for instance is not stored at this level but at the item level.
 */
public class ConfigRoot implements ConfigItemCollection {

    private final Extension extension;
    private final String prefix;

    private String overriddenDocFileName;
    private final List<AbstractConfigItem> items = new ArrayList<>();
    private final Set<String> qualifiedNames = new HashSet<>();

    public ConfigRoot(Extension extension, String prefix) {
        this.extension = extension;
        this.prefix = prefix;
    }

    public Extension getExtension() {
        return extension;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setOverriddenDocFileName(String overriddenDocFileName) {
        if (this.overriddenDocFileName != null) {
            return;
        }
        this.overriddenDocFileName = overriddenDocFileName;
    }

    public void addQualifiedName(String qualifiedName) {
        qualifiedNames.add(qualifiedName);
    }

    public Set<String> getQualifiedNames() {
        return Collections.unmodifiableSet(qualifiedNames);
    }

    @Override
    public void addItem(AbstractConfigItem item) {
        this.items.add(item);
    }

    @Override
    public List<AbstractConfigItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
