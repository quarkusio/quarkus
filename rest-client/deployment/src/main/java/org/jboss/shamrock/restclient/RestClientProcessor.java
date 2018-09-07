package org.jboss.shamrock.restclient;

import java.lang.reflect.Modifier;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;

import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ResteasyClientProxy;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.restclient.runtime.DefaultResponseExceptionMapper;
import org.jboss.shamrock.restclient.runtime.RestClientProxy;

class RestClientProcessor implements ResourceProcessor {

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    private ShamrockConfig config;

    private static final DotName[] CLIENT_ANNOTATIONS = {
            DotName.createSimple("javax.ws.rs.GET"),
            DotName.createSimple("javax.ws.rs.HEAD"),
            DotName.createSimple("javax.ws.rs.DELETE"),
            DotName.createSimple("javax.ws.rs.OPTIONS"),
            DotName.createSimple("javax.ws.rs.PATCH"),
            DotName.createSimple("javax.ws.rs.POST"),
            DotName.createSimple("javax.ws.rs.PUT"),
            DotName.createSimple("javax.ws.rs.PUT"),
            DotName.createSimple(RegisterRestClient.class.getName()),
            DotName.createSimple(Path.class.getName())
    };

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        processorContext.addReflectiveClass(false, false,
                DefaultResponseExceptionMapper.class.getName(),
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName());
        processorContext.addReflectiveClass(false, false, ClientRequestFilter[].class.getName());
        processorContext.addReflectiveClass(false, false, ClientResponseFilter[].class.getName());
        processorContext.addResource("META-INF/services/javax.ws.rs.ext.Providers");
        //TODO: fix this, we don't want to just add all the providers
        processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        processorContext.addProxyDefinition(ResteasyConfiguration.class.getName());
        for (DotName type : CLIENT_ANNOTATIONS) {
            for (AnnotationInstance annotation : archiveContext.getCombinedIndex().getAnnotations(type)) {
                AnnotationTarget target = annotation.target();
                ClassInfo theInfo;
                if (target.kind() == AnnotationTarget.Kind.CLASS) {
                    theInfo = target.asClass();
                } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    theInfo = target.asMethod().declaringClass();
                } else {
                    continue;
                }
                if (!Modifier.isInterface(theInfo.flags())) {
                    continue;
                }
                processorContext.addProxyDefinition( theInfo.name().toString(), ResteasyClientProxy.class.getName());
                processorContext.addProxyDefinition( theInfo.name().toString(), RestClientProxy.class.getName());
                processorContext.addReflectiveClass(true, false, theInfo.name().toString());

            }
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
