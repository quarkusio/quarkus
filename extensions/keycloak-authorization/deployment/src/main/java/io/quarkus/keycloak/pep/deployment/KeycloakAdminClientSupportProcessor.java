package io.quarkus.keycloak.pep.deployment;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientJsonMapper;
import io.quarkus.keycloak.pep.runtime.KeycloakAdminClientJackson2Bridge;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Support for the Keycloak admin client extensions when this extension is present.
 * Mitigates the fact that this extension is based on Jackson 2 and everything else in Quarkus is based on Jackson 3.
 */
public class KeycloakAdminClientSupportProcessor {

    private static final Logger LOG = Logger.getLogger(KeycloakAdminClientSupportProcessor.class);

    private static final String KEYCLOAK_GROUP_ID = "org.keycloak";
    private static final String JACKSON2_REPRESENTATIONS_ARTIFACT_ID = "keycloak-client-common-synced";
    private static final String JACKSON3_REPRESENTATIONS_ARTIFACT_ID = "keycloak-client-common-synced-jackson3";

    @BuildStep
    void adminClientJsonMapper(Capabilities capabilities, ServiceRegistrar serviceRegistrar,
            CurateOutcomeBuildItem curateOutcome, BuildProducer<RemovedResourceBuildItem> removedResources) {
        if (capabilities.isMissing(Capability.KEYCLOAK_ADMIN_CLIENT)) {
            return;
        }
        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        ResolvedDependency jackson2Representations = findRuntimeDependency(applicationModel,
                JACKSON2_REPRESENTATIONS_ARTIFACT_ID);
        ResolvedDependency jackson3Representations = findRuntimeDependency(applicationModel,
                JACKSON3_REPRESENTATIONS_ARTIFACT_ID);
        if (jackson2Representations != null && jackson3Representations != null) {
            Set<String> duplicatedClasses = new TreeSet<>(classes(jackson3Representations));
            duplicatedClasses.retainAll(classes(jackson2Representations));
            if (!duplicatedClasses.isEmpty()) {
                LOG.debugf("Removing %d classes duplicated in '%s' from '%s'", duplicatedClasses.size(),
                        jackson2Representations.toCompactCoords(), jackson3Representations.toCompactCoords());
                removedResources.produce(new RemovedResourceBuildItem(jackson3Representations.getKey(), duplicatedClasses));
            }
        }
        AdminClientJsonMapperService.register(serviceRegistrar);
    }

    /**
     * Registers the {@link KeycloakAdminClientJsonMapper} service and must only be used when the Keycloak admin client
     * extensions, which are optional dependencies of this extension, are present.
     * <p>
     * This code must stay out of the build step class: Quarkus loads all the methods of a build step class, including
     * the one the compiler generates for the lambda below, whose return type is {@link KeycloakAdminClientJsonMapper}.
     */
    private static final class AdminClientJsonMapperService {

        private static void register(ServiceRegistrar serviceRegistrar) {
            serviceRegistrar
                    .forService(KeycloakAdminClientJsonMapper.class)
                    .atPhase(Phase.INIT)
                    .onStart(ctx -> KeycloakAdminClientJackson2Bridge.jsonMapper());
        }
    }

    private static ResolvedDependency findRuntimeDependency(ApplicationModel applicationModel, String artifactId) {
        for (ResolvedDependency dependency : applicationModel.getRuntimeDependencies()) {
            if (KEYCLOAK_GROUP_ID.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return dependency;
            }
        }
        return null;
    }

    private static Set<String> classes(ResolvedDependency dependency) {
        Set<String> classes = new HashSet<>();
        dependency.getContentTree().walk(visit -> {
            String path = visit.getRelativePath("/");
            if (path.endsWith(".class")) {
                classes.add(path);
            }
        });
        return classes;
    }
}
