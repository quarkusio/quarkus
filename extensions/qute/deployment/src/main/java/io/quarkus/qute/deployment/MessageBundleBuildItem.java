package io.quarkus.qute.deployment;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.deployment.MessageBundleProcessor.MessageFile;

public final class MessageBundleBuildItem extends MultiBuildItem {

    private final String name;
    private final ClassInfo defaultBundleInterface;
    private final Map<String, ClassInfo> localizedInterfaces;
    private final Map<String, List<MessageFile>> localizedFiles;
    private final Map<String, List<MessageFile>> mergeCandidates;
    private final String defaultLocale;

    public MessageBundleBuildItem(String name, ClassInfo defaultBundleInterface,
            Map<String, ClassInfo> localizedInterfaces, Map<String, List<MessageFile>> localizedFiles,
            Map<String, List<MessageFile>> mergeCandidates, String defaultLocale) {
        this.name = name;
        this.defaultBundleInterface = defaultBundleInterface;
        this.localizedInterfaces = localizedInterfaces;
        this.localizedFiles = localizedFiles;
        this.mergeCandidates = mergeCandidates;
        this.defaultLocale = defaultLocale;
    }

    public String getName() {
        return name;
    }

    public ClassInfo getDefaultBundleInterface() {
        return defaultBundleInterface;
    }

    public Map<String, ClassInfo> getLocalizedInterfaces() {
        return localizedInterfaces;
    }

    public Map<String, List<MessageFile>> getLocalizedFiles() {
        return localizedFiles;
    }

    /**
     * Merge candidates are localized files used as a supplementary source of message templates
     * not specified by localized interfaces.
     */
    public Map<String, List<MessageFile>> getMergeCandidates() {
        return mergeCandidates;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }
}
