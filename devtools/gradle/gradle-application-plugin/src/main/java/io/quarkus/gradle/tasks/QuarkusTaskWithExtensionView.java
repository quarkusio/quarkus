package io.quarkus.gradle.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.app.ApplicationModelRelocation;

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
@DisableCachingByDefault(because = "Not cacheable")
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

    /**
     * The root directory of the build, used to resolve the relocation tokens of the serialized
     * application model back to absolute paths.
     * <p>
     * Declared {@code @Internal}: a root is what the model's paths are expressed relative <em>to</em>,
     * so letting it contribute to the cache key would put the checkout directory straight back into it.
     */
    @Internal
    public abstract DirectoryProperty getRootDirectory();

    /**
     * @see #getRootDirectory()
     */
    @Internal
    public abstract DirectoryProperty getGradleUserHomeDirectory();

    /**
     * The root directories of the builds included in this one, in declaration order, used to resolve
     * the tokens of the artifacts an included build contributed to the model. An included build lies
     * outside {@link #getRootDirectory()}, so it is relocated against a root of its own.
     * <p>
     * The order has to be the one the model was written with, since the roots are numbered by
     * position: both ends derive it from {@code Gradle.getIncludedBuilds()}.
     *
     * @see #getRootDirectory()
     */
    @Internal
    public abstract ListProperty<Directory> getIncludedBuildDirectories();

    /**
     * The roots to resolve the serialized application model's relocation tokens against: those every
     * reader derives from its environment, plus the directories this build knows.
     */
    protected List<ApplicationModelRelocation.Root> relocationRoots() {
        final List<ApplicationModelRelocation.Root> roots = new ArrayList<>();
        roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.BUILD_DIR_ROOT,
                getProjectLayout().getBuildDirectory().get().getAsFile().toPath()));
        roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.PROJECT_DIR_ROOT,
                getProjectLayout().getProjectDirectory().getAsFile().toPath()));
        if (getGradleUserHomeDirectory().isPresent()) {
            roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.GRADLE_USER_HOME_ROOT,
                    getGradleUserHomeDirectory().get().getAsFile().toPath()));
        }
        if (getRootDirectory().isPresent()) {
            roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.ROOT_DIR_ROOT,
                    getRootDirectory().get().getAsFile().toPath()));
        }
        final List<Directory> includedBuilds = getIncludedBuildDirectories().get();
        for (int i = 0; i < includedBuilds.size(); i++) {
            roots.add(new ApplicationModelRelocation.Root(
                    ApplicationModelRelocation.includedBuildRoot(i), includedBuilds.get(i).getAsFile().toPath()));
        }
        return ApplicationModelRelocation.withEnvironmentRoots(roots);
    }

    @Inject
    protected abstract ProjectLayout getProjectLayout();

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
