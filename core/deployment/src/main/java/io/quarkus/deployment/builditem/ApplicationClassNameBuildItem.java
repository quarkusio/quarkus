package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the fully qualified name of the application's entry point.
 * (<code>io.quarkus.runner.ApplicationImpl</code>) 
 * <p>
 * Other build steps can consume this item to generate code,
 * configure Native Image options, or adjust runtime behavior
 * based on the resolved application class.
 * </p>
 */
public final class ApplicationClassNameBuildItem extends SimpleBuildItem {
   
    private final String className;
    
    public ApplicationClassNameBuildItem(String className) {
        this.className = className;
    }
    
    public String getClassName() {
        return className;
    }
}
