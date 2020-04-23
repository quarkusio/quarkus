package io.quarkus.hibernate.orm.runtime.customized;

import java.util.LinkedHashSet;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;

/**
 * This is similar to the default {@link IntegratorService} from Hibernate ORM,
 * except that it doesn't come with default integrators as we prefer explicit
 * control.
 * 
 * @see org.hibernate.integrator.internal.IntegratorServiceImpl
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class QuarkusIntegratorServiceImpl implements IntegratorService {

    private final LinkedHashSet<Integrator> integrators = new LinkedHashSet<Integrator>();

    public QuarkusIntegratorServiceImpl(final ClassLoaderService classLoaderService) {
        integrators.addAll(classLoaderService.loadJavaServices(Integrator.class));
    }

    @Override
    public Iterable<Integrator> getIntegrators() {
        return integrators;
    }

}
