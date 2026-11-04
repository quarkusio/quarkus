package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Requires a class and its declared member classes, as returned by {@link Class#getDeclaredClasses()}, to be loadable
 * if another class is reachable. Loadable classes are kept together with their superclasses and interfaces,
 * but the jar tree shaker doesn't trace their code.
 * <p>
 * For example, {@code native-image} resolves {@code @TargetClass(className = "Outer", innerClass = "Inner")} by
 * looking up {@code Inner} in {@code Outer.getDeclaredClasses()}, which loads all member classes of {@code Outer}.
 * <p>
 * The condition class is evaluated like the one of {@link JarTreeShakeConditionalRootClassBuildItem}.
 */
public final class JarTreeShakeDeclaredClassesBuildItem extends MultiBuildItem {

    private final String conditionClassName;
    private final String holderClassName;

    /**
     * @param conditionClassName dot-separated name of the class whose reachability requires the declared classes
     * @param holderClassName dot-separated name of the class whose declared classes must be loadable
     */
    public JarTreeShakeDeclaredClassesBuildItem(String conditionClassName, String holderClassName) {
        this.conditionClassName = conditionClassName;
        this.holderClassName = holderClassName;
    }

    public String getConditionClassName() {
        return conditionClassName;
    }

    public String getHolderClassName() {
        return holderClassName;
    }
}
