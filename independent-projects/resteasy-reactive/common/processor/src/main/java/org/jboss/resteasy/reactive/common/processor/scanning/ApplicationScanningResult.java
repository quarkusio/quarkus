package org.jboss.resteasy.reactive.common.processor.scanning;

import java.util.Set;
import javax.ws.rs.core.Application;
import org.jboss.jandex.ClassInfo;
import org.jboss.resteasy.reactive.common.processor.BlockingDefault;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

public final class ApplicationScanningResult {

    final Set<String> allowedClasses;
    final Set<String> singletonClasses;
    final Set<String> excludedClasses;
    final Set<String> globalNameBindings;
    final boolean filterClasses;
    final Application application;
    final ClassInfo selectedAppClass;
    final BlockingDefault blocking;

    public ApplicationScanningResult(Set<String> allowedClasses, Set<String> singletonClasses, Set<String> excludedClasses,
            Set<String> globalNameBindings, boolean filterClasses, Application application,
            ClassInfo selectedAppClass, BlockingDefault blocking) {
        this.allowedClasses = allowedClasses;
        this.singletonClasses = singletonClasses;
        this.excludedClasses = excludedClasses;
        this.globalNameBindings = globalNameBindings;
        this.filterClasses = filterClasses;
        this.application = application;
        this.selectedAppClass = selectedAppClass;
        this.blocking = blocking;
    }

    public KeepProviderResult keepProvider(ClassInfo providerClass) {
        if (filterClasses) {
            // we don't care about provider annotations, they're manually registered (but for the server only)
            if (allowedClasses.isEmpty()) {
                // we only have only classes to exclude
                return excludedClasses.contains(providerClass.name().toString()) ? KeepProviderResult.DISCARD
                        : KeepProviderResult.SERVER_ONLY;
            }
            return allowedClasses.contains(providerClass.name().toString()) ? KeepProviderResult.SERVER_ONLY
                    : KeepProviderResult.DISCARD;
        }
        return providerClass.classAnnotation(ResteasyReactiveDotNames.PROVIDER) != null ? KeepProviderResult.NORMAL
                : KeepProviderResult.DISCARD;
    }

    public boolean keepClass(String className) {
        if (filterClasses) {
            if (allowedClasses.isEmpty()) {
                // we only have only classes to exclude
                return !excludedClasses.contains(className);
            }
            return allowedClasses.contains(className);
        }
        return true;
    }

    public Set<String> getAllowedClasses() {
        return allowedClasses;
    }

    public Set<String> getExcludedClasses() {
        return excludedClasses;
    }

    public Set<String> getSingletonClasses() {
        return singletonClasses;
    }

    public Set<String> getGlobalNameBindings() {
        return globalNameBindings;
    }

    public boolean isFilterClasses() {
        return filterClasses;
    }

    public Application getApplication() {
        return application;
    }

    public ClassInfo getSelectedAppClass() {
        return selectedAppClass;
    }

    public BlockingDefault getBlockingDefault() {
        return blocking;
    }

    public enum KeepProviderResult {
        NORMAL,
        SERVER_ONLY,
        DISCARD
    }

}
