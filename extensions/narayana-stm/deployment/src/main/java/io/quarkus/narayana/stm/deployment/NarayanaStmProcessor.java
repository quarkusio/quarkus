package io.quarkus.narayana.stm.deployment;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.stm.annotations.Transactional;

import com.arjuna.ats.internal.arjuna.coordinator.CheckedActionFactoryImple;
import com.arjuna.ats.txoj.Lock;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;

class NarayanaStmProcessor {
    private static final Logger log = Logger.getLogger(NarayanaStmProcessor.class.getName());

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    // register classes in need of reflection
    @BuildStep
    ReflectiveClassBuildItem register(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.NARAYANA_STM));

        return new ReflectiveClassBuildItem(true, false,
                CheckedActionFactoryImple.class.getName(),
                Lock.class.getName());
    }

    // register STM dynamic proxies
    @BuildStep
    SubstrateProxyDefinitionBuildItem stmProxies() {
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
                            new ReflectiveHierarchyBuildItem(Type.create(ci.name(), Type.Kind.CLASS)));
                }
            }
        }

        String[] classNames = proxies.toArray(new String[0]);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, classNames));

        return new SubstrateProxyDefinitionBuildItem(classNames);
    }
}
