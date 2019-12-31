package io.quarkus.cxf.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.jboss.logging.Logger;

public class CXFQuarkusServlet extends CXFNonSpringServlet {

    private static final Logger LOGGER = Logger.getLogger(CXFQuarkusServlet.class);

    private static final List<CXFServletInfo> WEB_SERVICES = new ArrayList<>();

    private Object loadClass(String className) {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            LOGGER.warn(e);
        } catch (IllegalAccessException e) {
            LOGGER.warn(e);
        } catch (InvocationTargetException e) {
            LOGGER.warn(e);
        } catch (NoSuchMethodException e) {
            LOGGER.warn(e);
        } catch (ClassNotFoundException e) {
            LOGGER.warn(e);
        }
        return null;
    }

    @Override
    public void loadBus(ServletConfig servletConfig) {
        LOGGER.info("Load CXF bus");
        super.loadBus(servletConfig);

        Bus bus = getBus();
        BusFactory.setDefaultBus(bus);

        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setBus(bus);
        for (CXFServletInfo servletInfo : WEB_SERVICES) {
            Object instanceService = loadClass(servletInfo.getClassName());
            if (instanceService != null) {
                factory.setServiceBean(instanceService);
                factory.setAddress(servletInfo.getPath());
                if (servletInfo.getFeatures().size() > 0) {
                    List<Feature> features = new ArrayList<>();
                    for (String feature : servletInfo.getFeatures()) {
                        Feature instanceFeature = (Feature) loadClass(feature);
                        features.add(instanceFeature);
                    }
                    factory.setFeatures(features);
                }

                Server server = factory.create();
                for (String className : servletInfo.getInFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) loadClass(className);
                    server.getEndpoint().getInFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getInInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) loadClass(className);
                    server.getEndpoint().getInInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutFaultInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) loadClass(className);
                    server.getEndpoint().getOutFaultInterceptors().add(interceptor);
                }
                for (String className : servletInfo.getOutInterceptors()) {
                    Interceptor<? extends Message> interceptor = (Interceptor<? extends Message>) loadClass(className);
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
