package io.quarkus.elytron.security.filesystem.deployment;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.filesystem.runtime.FilesystemRealmConfig;
import io.quarkus.elytron.security.filesystem.runtime.FilesystemRecorder;
import io.quarkus.runtime.RuntimeValue;

class FilesystemDeploymentProcessor {

    FilesystemRealmConfig filesystem;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.SECURITY_ELYTRON_FILESYSTEM);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY_FILESYSTEM);
    }

    /**
     * Check to see if a {@link FilesystemRealmConfig} was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.FileSystemSecurityRealm}
     * runtime value to process the user/roles files.
     *
     * @param recorder - runtime security recorder
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureFilesystemRealmAuthConfig(FilesystemRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm) {
        if (filesystem.enabled) {
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(filesystem);
            securityRealm.produce(new SecurityRealmBuildItem(realm, filesystem.realmName, null));
        }
    }

    @BuildStep
    ElytronPasswordMarkerBuildItem marker() {
        if (filesystem.enabled) {
            return new ElytronPasswordMarkerBuildItem();
        }
        return null;
    }
}
