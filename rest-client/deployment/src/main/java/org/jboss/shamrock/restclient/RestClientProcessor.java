package org.jboss.shamrock.restclient;

import java.lang.reflect.Modifier;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.ShamrockConfig;

class RestClientProcessor implements ResourceProcessor {

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    private ShamrockConfig config;


    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        int count = 0;
        for(AnnotationInstance annotation : archiveContext.getCombinedIndex().getAnnotations(REGISTER_REST_CLIENT)) {
            AnnotationTarget target = annotation.target();
            ClassInfo clazz = target.asClass();
            if(!Modifier.isInterface(clazz.flags())) {
                throw new RuntimeException("RegisterRestClient can only be applied to interfaces: " + clazz.name());
            }

        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
