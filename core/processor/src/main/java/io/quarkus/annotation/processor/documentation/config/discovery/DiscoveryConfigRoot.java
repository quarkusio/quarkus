package io.quarkus.annotation.processor.documentation.config.discovery;

import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;
import io.quarkus.annotation.processor.documentation.config.model.Extension;

/**
 * At this stage, each {@code @ConfigRoot} annotation leads to a separate DiscoveryConfigRoot.
 * So you basically get one DiscoveryConfigRoot per phase.
 * The config roots will get merged when we resolve the final model.
 */
public final class DiscoveryConfigRoot extends DiscoveryRootElement {

    private final String prefix;
    private final String overriddenDocPrefix;
    private final ConfigPhase phase;
    private final String overriddenDocFileName;

    public DiscoveryConfigRoot(Extension extension, String prefix, String overriddenDocPrefix,
            String binaryName, String qualifiedName,
            ConfigPhase configPhase, String overriddenDocFileName, boolean configMapping) {
        super(extension, binaryName, qualifiedName, configMapping);

        this.prefix = prefix;
        this.overriddenDocPrefix = overriddenDocPrefix;
        this.phase = configPhase;
        this.overriddenDocFileName = overriddenDocFileName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getOverriddenDocPrefix() {
        return overriddenDocPrefix;
    }

    public ConfigPhase getPhase() {
        return phase;
    }

    public String getOverriddenDocFileName() {
        return overriddenDocFileName;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "prefix = " + this.prefix + "\n");
        sb.append(prefix + "config = " + this.phase + "\n");
        if (overriddenDocFileName != null) {
            sb.append(prefix + "overriddenDocFileName = " + this.overriddenDocFileName + "\n");
        }
        sb.append(super.toString(prefix));

        return sb.toString();
    }
}
