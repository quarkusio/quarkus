package io.quarkus.smallrye.jwt.build.deployment;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.impl.JwtBuildUtils;
import io.smallrye.jwt.build.impl.JwtProviderImpl;

class SmallRyeJwtBuildProcessor {

    private static final Logger log = Logger.getLogger(SmallRyeJwtBuildProcessor.class.getName());

    @BuildStep
    void addClassesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses
                .produce(ReflectiveClassBuildItem
                        .builder(SignatureAlgorithm.class, KeyEncryptionAlgorithm.class, JwtProviderImpl.class)
                        .reason(getClass().getName())
                        .methods().fields().build());
    }

    /**
     * If the configuration specified a deployment local key resource, register it in native mode
     *
     * @return NativeImageResourceBuildItem
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerNativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        Config config = ConfigProvider.getConfig();
        registerKeyLocationResource(config, JwtBuildUtils.SIGN_KEY_LOCATION_PROPERTY, nativeImageResource);
        registerKeyLocationResource(config, JwtBuildUtils.ENC_KEY_LOCATION_PROPERTY, nativeImageResource);
    }

    private void registerKeyLocationResource(Config config, String propertyName,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        Optional<String> keyLocation = config.getOptionalValue(propertyName, String.class);
        if (keyLocation.isPresent() && keyLocation.get().length() > 1
                && (keyLocation.get().indexOf(':') < 0 || keyLocation.get().startsWith("classpath:"))) {
            log.infof("Adding %s to native image", keyLocation.get());
            String location = keyLocation.get().startsWith("/") ? keyLocation.get().substring(1) : keyLocation.get();
            nativeImageResource.produce(new NativeImageResourceBuildItem(location));
        }
    }
}
