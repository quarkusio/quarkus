package io.quarkus.gradle.tasks;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

/**
 * Quarkus task providing inputs compatible with the configuration cache, used by the {@link QuarkusGenerateCode}
 * and {@link QuarkusBuildTask} tasks.
 * <p>
 * Most inputs are provided by the {@link QuarkusPluginExtensionView}. This includes those required by both tasks,
 * and additional inputs that require initialization of the {@link BaseConfig} object.
 * </p>
 * <p>
 * Additionally, this class provides an {@link EffectiveConfigProvider}, which is used by dependent tasks
 * to access the inputs defined in this task.
 * </p>
 */
public abstract class QuarkusTaskWithExtensionView extends QuarkusTask {

    private final QuarkusPluginExtensionView extensionView;

    @Input
    @Optional
    public abstract MapProperty<String, Object> getManifestAttributes();

    @Input
    @Optional
    public abstract MapProperty<String, Attributes> getManifestSections();

    @Input
    public abstract MapProperty<String, String> getCachingRelevantInput();

    public QuarkusTaskWithExtensionView(String description, boolean compatible) {
        super(description, compatible);
        this.extensionView = getProject().getObjects().newInstance(QuarkusPluginExtensionView.class, extension());
    }

    public EffectiveConfigProvider effectiveProvider() {
        return new EffectiveConfigProvider(
                getExtensionView().getIgnoredEntries(),
                getExtensionView().getMainResources(),
                getExtensionView().getForcedProperties(),
                getExtensionView().getProjectProperties(),
                getExtensionView().getQuarkusBuildProperties(),
                getExtensionView().getQuarkusRelevantProjectProperties(),
                getManifestAttributes(),
                getManifestSections(),
                getExtensionView().getNativeBuild(),
                getExtensionView().getQuarkusProfileSystemVariable(),
                getExtensionView().getQuarkusProfileEnvVariable());
    }

    /**
     * Returns a view of the Quarkus extension that is compatible with the configuration cache.
     */
    @Nested
    protected QuarkusPluginExtensionView getExtensionView() {
        return extensionView;
    }
}
