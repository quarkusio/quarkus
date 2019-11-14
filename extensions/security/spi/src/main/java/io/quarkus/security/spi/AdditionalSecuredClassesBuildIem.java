package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Collections;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalSecuredClassesBuildIem extends MultiBuildItem {
    public final Collection<ClassInfo> additionalSecuredClasses;

    public AdditionalSecuredClassesBuildIem(Collection<ClassInfo> additionalSecuredClasses) {
        this.additionalSecuredClasses = Collections.unmodifiableCollection(additionalSecuredClasses);
    }
}
