
package io.quarkus.gradle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;

public class GradleUtils {

    public static String getQuarkusCoreVersion(Project project) {
        final List<Dependency> bomDeps = listProjectBoms(project);
        if (bomDeps.isEmpty()) {
            throw new GradleException("No platforms detected in the project");
        }

        final Configuration boms = project.getConfigurations()
                .detachedConfiguration(bomDeps.toArray(new org.gradle.api.artifacts.Dependency[0]));

        final AtomicReference<String> quarkusVersionRef = new AtomicReference<>();
        boms.getResolutionStrategy().eachDependency(d -> {
            if (quarkusVersionRef.get() == null && d.getTarget().getName().equals("quarkus-core")
                    && d.getTarget().getGroup().equals("io.quarkus")) {
                quarkusVersionRef.set(d.getTarget().getVersion());
            }
        });
        boms.getResolvedConfiguration();
        final String quarkusCoreVersion = quarkusVersionRef.get();
        if (quarkusCoreVersion == null) {
            throw new IllegalStateException("Failed to determine the Quarkus core version for the project");
        }
        return quarkusCoreVersion;
    }

    public static List<Dependency> listProjectBoms(Project project) {
        final Configuration impl = project.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
        List<Dependency> boms = new ArrayList<>();
        impl.getIncoming().getDependencies().forEach(d -> {
            if (!(d instanceof ModuleDependency)) {
                return;
            }
            final ModuleDependency module = (ModuleDependency) d;
            final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
            if (category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                    || Category.REGULAR_PLATFORM.equals(category.getName()))) {
                boms.add(d);
            }
        });
        return boms;
    }

}
