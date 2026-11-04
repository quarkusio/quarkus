package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A class that the jar tree shaker must keep if another class is reachable.
 * <p>
 * The reachability of dependency classes is determined by the analysis. Application and generated classes,
 * classes of artifacts excluded from tree shaking, and JDK and GraalVM classes are always present, so a class conditional on
 * one of them is always kept. A class conditional on a class that isn't on the classpath at all is removed,
 * since its condition is not met.
 */
public final class JarTreeShakeConditionalRootClassBuildItem extends MultiBuildItem {

    private final String conditionClassName;
    private final String className;

    /**
     * @param conditionClassName dot-separated name of the class whose reachability makes {@code className} reachable
     * @param className dot-separated name of the conditionally reachable class
     */
    public JarTreeShakeConditionalRootClassBuildItem(String conditionClassName, String className) {
        this.conditionClassName = conditionClassName;
        this.className = className;
    }

    public String getConditionClassName() {
        return conditionClassName;
    }

    public String getClassName() {
        return className;
    }
}
