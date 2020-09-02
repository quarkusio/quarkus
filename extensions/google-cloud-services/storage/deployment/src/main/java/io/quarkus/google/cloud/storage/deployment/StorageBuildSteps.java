package io.quarkus.google.cloud.storage.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gcp.services.storage.runtime.StorageProducer;

public class StorageBuildSteps {
    private static final String FEATURE = "google-cloud-storage";

    private static final DotName DOTNAME_GENERIC_JSON = DotName.createSimple("com.google.api.client.json.GenericJson");

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(StorageProducer.class);
    }

    @BuildStep
    public IndexDependencyBuildItem indexModelDependency() {
        // index the 'google-api-services-storage' library as it contains all model classes that needs to be registered for reflection
        return new IndexDependencyBuildItem("com.google.apis", "google-api-services-storage");
    }

    @BuildStep
    public void registerModelForReflection(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItem) {
        // The BigQuery library uses request classes that extends the GenericJson class and are then serialized in JSON using reflection.
        Collection<ClassInfo> allKnownImplementors = combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DOTNAME_GENERIC_JSON);
        for (ClassInfo candidate : allKnownImplementors) {
            DotName dotName = candidate.name();
            Type jandexType = Type.create(dotName, Type.Kind.CLASS);
            reflectiveClassBuildItem.produce(new ReflectiveClassBuildItem(true, true, jandexType.name().toString()));
        }
    }

    @BuildStep
    public ReflectiveClassBuildItem registerForReflection() {
        Set<Class> classes = new HashSet<>();
        // we register for reflection all inner classes and the inner classes fo those inner classes
        // we may register too many classes but we don't really have the choice here.
        gatherInnerClasses(classes, com.google.api.services.storage.Storage.class.getDeclaredClasses());
        return new ReflectiveClassBuildItem(true, true, classes.toArray(new Class[] {}));
    }

    private void gatherInnerClasses(Set<Class> classes, Class[] registerForReflection) {
        for (Class<?> theClass : registerForReflection) {
            // add the inner classes to register for reflection collection
            Class<?>[] declaredClasses = theClass.getDeclaredClasses();
            if (declaredClasses.length > 0) {
                classes.addAll(Arrays.asList(declaredClasses));
                // there exist some inner classes that have inner classes ...
                gatherInnerClasses(classes, declaredClasses);
            }
        }
    }
}
