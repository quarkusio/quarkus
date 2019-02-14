package org.jboss.shamrock.extest;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.jboss.shamrock.arc.deployment.BeanArchiveIndexBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanDefiningAnnotationBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;

/**
 * A test extension deployment processor
 */
public class TestProcessor {
    static final Logger log = Logger.getLogger(TestProcessor.class);
    static DotName TEST_ANNOTATION = DotName.createSimple(TestAnnotation.class.getName());

    TestBuildTimeConfig buildTimeConfig;
    TestRunTimeConfig runTimeConfig;

    @BuildStep(providesCapabilities = "org.jboss.shamrock.test")
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.CDI);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem registerX() {
        return new BeanDefiningAnnotationBuildItem(TEST_ANNOTATION);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void checkConfig() {
        if(!buildTimeConfig.btSBV.getValue().equals("StringBasedValue")) {
            throw new IllegalStateException("buildTimeConfig.btSBV != StringBasedValue");
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureBean(TestTemplate template, BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> testBeans = indexView.getAnnotations(TEST_ANNOTATION);
        for(AnnotationInstance ann : testBeans) {
            ClassInfo beanClassInfo = ann.target().asClass();
            try {
                Class<?> beanClass = Class.forName(beanClassInfo.name().toString());
                template.configureBeans(beanClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
