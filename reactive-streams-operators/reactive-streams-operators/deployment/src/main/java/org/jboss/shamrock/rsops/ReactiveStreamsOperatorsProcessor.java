package org.jboss.shamrock.rsops;

import io.smallrye.reactive.streams.Engine;
import org.eclipse.microprofile.reactive.streams.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;

public class ReactiveStreamsOperatorsProcessor {

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                      BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ReactiveStreamsFactoryImpl.class.getName(), Engine.class.getName()));

        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + ReactiveStreamsEngine.class.getName()));
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + ReactiveStreamsFactory.class.getName()));
    }

}
