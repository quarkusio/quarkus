package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This will generate the equivalent of "--add-opens=[openedModule/package(s)]=[openingModule]" for
 * all runners of the generated application.
 * Some limitations apply which will depend on the launch mode of Quarkus; specifically, when
 * generating a runnable Jar we can only open a module to ALL-UNNAMED.
 */
public final class ModuleOpenBuildItem extends MultiBuildItem {

    /**
     * Use this specific constant to open a module to "the" unname module of the App.
     * There are normally multiple "unnamed modules" in the JVM; picking this specific name
     * in the context of this build item requests that the specific unnamed module of the
     * classloader running Quarkus will be instrumented with the matching requests.
     * When new classloaders are defined (e.g. running in live-reload mode), the rules
     * will be applied again.
     */
    public static final String ALL_UNNAMED = "ALL-UNNAMED";

    private final String openedModuleName;
    private final String openingModuleName;
    private final Set<String> packageNames;

    /**
     * Create a new ModuleOpenBuildItem instance.
     *
     * @param openedModuleName The name of the module which needs to be opened.
     * @param openingModuleName The name of the module which is being granted access.
     * @param packageNames The packages which are being opened. At least one must be specified.
     */
    public ModuleOpenBuildItem(String openedModuleName, String openingModuleName, String... packageNames) {
        //Validating for actual module existence is deferred: such aspects are classloader specific,
        //and these rules might need to be applied on a variety of classloaders.
        this.openedModuleName = Objects.requireNonNull(openedModuleName);
        this.openingModuleName = Objects.requireNonNull(openingModuleName);
        this.packageNames = Set.of(packageNames);
        if (packageNames.length == 0) {
            throw new IllegalArgumentException("At least one package name must be specified");
        }
    }

    public String openedModuleName() {
        return openedModuleName;
    }

    public String openingModuleName() {
        return openingModuleName;
    }

    public Set<String> packageNames() {
        return packageNames;
    }

    @Override
    public String toString() {
        //Used for diagnostic logging purposes
        return "ModuleOpenBuildItem{" +
                "openedModule='" + openedModuleName + '\'' +
                ", openingModule='" + openingModuleName + '\'' +
                ", packages=" + packageNames +
                '}';
    }

}
