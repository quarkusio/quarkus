package io.quarkus.liquibase.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.graal.LiquibaseServiceLoader;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;
import liquibase.exception.ServiceNotFoundException;
import liquibase.servicelocator.ServiceLocator;

/**
 * The liquibase recorder
 */
@Recorder
public class LiquibaseRecorder {

    private static final Logger log = Logger.getLogger(LiquibaseRecorder.class);

    private final List<LiquibaseContainer> liquibaseContainers = new ArrayList<>(2);

    public void setServicesImplementations(Map<String, List<String>> serviceLoader) {
        LiquibaseServiceLoader.setServicesImplementations(serviceLoader);
    }

    public Supplier<LiquibaseFactory> liquibaseSupplier(String dataSourceName) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        LiquibaseContainerProducer liquibaseProducer = Arc.container().instance(LiquibaseContainerProducer.class).get();
        LiquibaseContainer liquibaseContainer = liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
        liquibaseContainers.add(liquibaseContainer);
        return new Supplier<LiquibaseFactory>() {
            @Override
            public LiquibaseFactory get() {
                return liquibaseContainer.getLiquibaseFactory();
            }
        };
    }

    public void doStartActions() {
        try {
            for (LiquibaseContainer liquibaseContainer : liquibaseContainers) {
                LiquibaseFactory liquibaseFactory = liquibaseContainer.getLiquibaseFactory();
                if (liquibaseContainer.isCleanAtStart()) {
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        liquibase.dropAll();
                    }
                }
                if (liquibaseContainer.isMigrateAtStart()) {
                    if (liquibaseContainer.isValidateOnMigrate()) {
                        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                            liquibase.validate();
                        }
                    }
                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void setJvmServiceImplementations(Map<String, List<String>> services) {
        ServiceLocator.setInstance(new ServiceLocator() {

            @Override
            public <T> Class<? extends T>[] findClasses(Class<T> requiredInterface) throws ServiceNotFoundException {
                List<String> found = services.get(requiredInterface.getName());
                if (found == null) {
                    log.warnf("Failed to find pre-indexed service %s, falling back to slow classpath scanning",
                            requiredInterface);
                    return super.findClasses(requiredInterface);
                }
                List<Class<? extends T>> ret = new ArrayList<>();
                for (String i : found) {
                    try {
                        ret.add((Class<? extends T>) Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
                    } catch (ClassNotFoundException e) {
                        log.error("Failed to load Liquibase service", e);
                    }
                }
                return ret.toArray(new Class[0]);
            }
        });
    }
}
