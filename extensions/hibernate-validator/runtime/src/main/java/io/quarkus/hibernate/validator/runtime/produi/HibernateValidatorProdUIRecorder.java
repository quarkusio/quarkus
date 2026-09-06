package io.quarkus.hibernate.validator.runtime.produi;

import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Seeds the {@link HibernateValidatorProdUIService} with the set of validated
 * class names discovered at build time. The names are looked up and introspected
 * lazily (read-only) when the Prod UI page is opened.
 */
@Recorder
public class HibernateValidatorProdUIRecorder {

    public void initializeProdUIService(List<String> validatedClassNames) {
        InstanceHandle<HibernateValidatorProdUIService> handle = Arc.container()
                .instance(HibernateValidatorProdUIService.class);
        if (handle.isAvailable()) {
            handle.get().setValidatedClassNames(validatedClassNames);
        }
    }
}
