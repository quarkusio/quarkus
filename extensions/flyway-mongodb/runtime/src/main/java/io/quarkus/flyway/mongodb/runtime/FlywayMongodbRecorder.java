package io.quarkus.flyway.mongodb.runtime;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayMongodbRecorder {

    private static final Logger LOG = Logger.getLogger(FlywayMongodbRecorder.class);

    private final RuntimeValue<FlywayMongodbRuntimeConfig> runtimeConfig;

    public FlywayMongodbRecorder(RuntimeValue<FlywayMongodbRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void setApplicationMigrationFiles(Map<String, Collection<String>> filesByClient) {
        LOG.debugv("Setting the following application migration files: {0}", filesByClient);
        QuarkusMongodbPathLocationScanner.setApplicationMigrationFiles(filesByClient);
    }

    public Function<SyntheticCreationalContext<FlywayMongodbContainer>, FlywayMongodbContainer> flywayMongodbContainerFunction(
            String clientName, boolean hasMigrations, Set<String> resourcesLocations) {
        return new Function<>() {
            @Override
            public FlywayMongodbContainer apply(SyntheticCreationalContext<FlywayMongodbContainer> context) {
                FlywayMongodbContainerProducer producer = context.getInjectedReference(FlywayMongodbContainerProducer.class);
                return producer.createFlywayContainer(clientName, hasMigrations, resourcesLocations);
            }
        };
    }

    public Function<SyntheticCreationalContext<Flyway>, Flyway> flywayFunction(String clientName) {
        return new Function<>() {
            @Override
            public Flyway apply(SyntheticCreationalContext<Flyway> context) {
                FlywayMongodbContainer container = context.getInjectedReference(FlywayMongodbContainer.class,
                        FlywayMongodbContainerUtil.getQualifier(clientName));
                return container.flyway();
            }
        };
    }

    public Supplier<ActiveResult> flywayCheckActiveSupplier(String clientName) {
        return new Supplier<ActiveResult>() {
            @Override
            public ActiveResult get() {
                FlywayMongodbClientRuntimeConfig clientConfig = runtimeConfig.getValue().clients().get(clientName);
                if (clientConfig != null && clientConfig.active().isPresent() && !clientConfig.active().get()) {
                    String propName = MongoConfig.isDefaultClient(clientName)
                            ? "quarkus.flyway-mongodb.active"
                            : "quarkus.flyway-mongodb." + clientName + ".active";
                    return ActiveResult.inactive(String.format(Locale.ROOT,
                            "Flyway MongoDB for client '%s' was deactivated through configuration. "
                                    + "To activate it set '%s' to 'true'.",
                            clientName, propName));
                }
                var mongoClientInstance = MongoClientBeanUtil.mongoClientInstance(clientName);
                if (!mongoClientInstance.isUnsatisfied()) {
                    var mongoClientBean = mongoClientInstance.getHandle().getBean();
                    var mongoClientActive = mongoClientBean.checkActive();
                    if (!mongoClientActive.value()) {
                        return ActiveResult.inactive(
                                String.format(Locale.ROOT,
                                        "Flyway MongoDB for client '%s' was deactivated automatically because the MongoDB client was deactivated.",
                                        clientName),
                                mongoClientActive);
                    }
                }
                return ActiveResult.active();
            }
        };
    }

    public void doStartActions(String clientName) {
        ActiveResult active = flywayCheckActiveSupplier(clientName).get();
        if (!active.value()) {
            LOG.debugf("Skipping Flyway-MongoDB start actions for client '%s': %s",
                    clientName, active.inactiveReason());
            return;
        }
        FlywayMongodbContainer container = FlywayMongodbContainerUtil.getFlywayMongodbContainer(clientName);
        if (container == null) {
            LOG.debugf("Skipping Flyway-MongoDB start actions for client '%s': no FlywayMongodbContainer bean is resolvable.",
                    clientName);
            return;
        }
        Flyway flyway = container.flyway();

        if (container.cleanAtStart()) {
            LOG.debugf("Running clean for Flyway-MongoDB client '%s'", clientName);
            flyway.clean();
        }

        if (container.validateAtStart()) {
            LOG.debugf("Running validate for Flyway-MongoDB client '%s'", clientName);
            if (container.cleanOnValidationError()) {
                var result = flyway.validateWithResult();
                if (!result.validationSuccessful) {
                    LOG.warnf("Validation failed for Flyway-MongoDB client '%s'; cleaning before migrate. Cause: %s",
                            clientName, result.errorDetails);
                    flyway.clean();
                }
            } else {
                flyway.validate();
            }
        }

        if (container.baselineAtStart()) {
            LOG.debugf("Running baseline for Flyway-MongoDB client '%s'", clientName);
            if (flyway.info().applied().length == 0) {
                flyway.baseline();
            }
        }

        if (container.repairAtStart()) {
            LOG.debugf("Running repair for Flyway-MongoDB client '%s'", clientName);
            flyway.repair();
        }

        if (container.migrateAtStart()) {
            LOG.debugf("Running migrate for Flyway-MongoDB client '%s'", clientName);
            flyway.migrate();
        }
    }
}
