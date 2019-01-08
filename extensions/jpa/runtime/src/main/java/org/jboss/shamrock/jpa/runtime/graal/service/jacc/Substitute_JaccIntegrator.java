package org.jboss.shamrock.jpa.runtime.graal.service.jacc;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.secure.internal.DisabledJaccServiceImpl;
import org.hibernate.secure.spi.JaccService;
import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.secure.spi.JaccIntegrator")
public final class Substitute_JaccIntegrator {

    @Alias
    private static Logger log;

    @Substitute
    public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        boolean isSecurityEnabled = serviceRegistryBuilder.getSettings().containsKey( AvailableSettings.JACC_ENABLED );
        if (isSecurityEnabled) {
            log.warn("This build of Hibernate ORM doesn't support JACC");
        }

        final JaccService jaccService = new DisabledJaccServiceImpl();
        serviceRegistryBuilder.addService( JaccService.class, jaccService );
    }
}
