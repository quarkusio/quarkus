package io.quarkus.narayana.stm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.stm.annotations.Transactional;

import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;
import com.arjuna.ats.internal.arjuna.utils.SocketProcessId;
import com.arjuna.ats.txoj.Lock;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.narayana.stm.runtime.NarayanaSTMRecorder;

class NarayanaSTMProcessor {
    private static final Logger log = Logger.getLogger(NarayanaSTMProcessor.class.getName());

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    // register classes in need of reflection
    @BuildStep
    ReflectiveClassBuildItem registerFeature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.NARAYANA_STM));

        return new ReflectiveClassBuildItem(true, false,
                ShadowNoFileLockStore.class.getName(),
                CheckedActionFactoryImple.class.getName(),
                Lock.class.getName());
    }

    // the software transactional memory implementation does not require a TSM
    // so disable it at native image build time
    @BuildStep()
    public NativeImageSystemPropertyBuildItem substrateSystemPropertyBuildItem() {
        return new NativeImageSystemPropertyBuildItem("CoordinatorEnvironmentBean.transactionStatusManagerEnable", "false");
    }

    // the software transactional memory implementation does not require a TSM
    // so disable it at runtime
    @BuildStep()
    @Record(RUNTIME_INIT)
    public void configureRuntimeProperties(NarayanaSTMRecorder recorder,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        recorder.disableTransactionStatusManager();
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(SocketProcessId.class.getName()));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(Lock.class.getName()));
    }

    // register STM dynamic proxies
    @BuildStep
    NativeImageProxyDefinitionBuildItem stmProxies(
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final DotName TRANSACTIONAL = DotName.createSimple(Transactional.class.getName());
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<String> proxies = new ArrayList<>();

        for (AnnotationInstance stm : index.getAnnotations(TRANSACTIONAL)) {
            if (AnnotationTarget.Kind.CLASS.equals(stm.target().kind())) {
                DotName name = stm.target().asClass().name();

                proxies.add(name.toString());

                log.debugf("Registering transactional interface %s%n", name);

                for (ClassInfo ci : index.getAllKnownImplementors(name)) {
                    reflectiveHierarchyClass.produce(
                            new ReflectiveHierarchyBuildItem.Builder()
                                    .type(Type.create(ci.name(), Type.Kind.CLASS))
                                    .source(getClass().getSimpleName() + " > " + ci.name())
                                    .build());
                }
            }
        }

        String[] classNames = proxies.toArray(new String[0]);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, classNames));

        return new NativeImageProxyDefinitionBuildItem(classNames);
    }
}
