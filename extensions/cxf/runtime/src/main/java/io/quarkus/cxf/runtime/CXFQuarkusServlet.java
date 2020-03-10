package io.quarkus.cxf.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;

public class CXFQuarkusServlet extends CXFNonSpringServlet {

    private static final Logger LOGGER = Logger.getLogger(CXFQuarkusServlet.class);

    private static final List<CXFServletInfo> WEB_SERVICES = new ArrayList<>();

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("failed to load class " + className);
            return null;
        }
    }

    private Object getIntance(String className) {
        return ((ClientProxy) Arc.container().instance(className).get()).arc_contextualInstance();
    }

    @Override
    public void loadBus(ServletConfig servletConfig) {
        LOGGER.info("Load CXF bus");
        super.loadBus(servletConfig);

        Bus bus = getBus();
        BusFactory.setDefaultBus(bus);

        //ServerFactoryBean factory = new ServerFactoryBean();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);

        for (CXFServletInfo servletInfo : WEB_SERVICES) {
            Object instanceService = getIntance(servletInfo.getClassName());
            if (instanceService != null) {
                Class<?> seiClass = null;
                if (servletInfo.getSei() != null) {
                    seiClass = loadClass(servletInfo.getSei());
                    factory.setServiceClass(seiClass);
                }
                if (seiClass == null) {
                    LOGGER.warn("sei not found: " + servletInfo.getSei());
                }
                factory.setAddress(servletInfo.getPath());
                factory.setServiceBean(instanceService);
                if (servletInfo.getWsdlPath() != null) {
                    factory.setWsdlLocation(servletInfo.getWsdlPath());
                }
                if (servletInfo.getFeatures().size() > 0) {
                    List<Feature> features = new ArrayList<>();
                    for (String feature : servletInfo.getFeatures()) {
                        Feature instanceFeature = (Feature) getIntance(feature);
                        features.add(instanceFeature);
                    }
                    factory.setFeatures(features);
                }

                Server server = factory.create();
                for (String className : servletInfo.getInFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getIntance(className);
                    server.getEndpoint().getInFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getInInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getIntance(className);
                    server.getEndpoint().getInInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getIntance(className);
                    server.getEndpoint().getOutFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) getIntance(className);
                    server.getEndpoint().getOutInterceptors().add(interceptor);
                }

                LOGGER.info(servletInfo.toString() + " available.");
            } else {
                LOGGER.error("Cannot initialize " + servletInfo.toString());
            }
        }
    }

    public static void publish(CXFServletInfo cfg) {
        WEB_SERVICES.add(cfg);
    }
}
