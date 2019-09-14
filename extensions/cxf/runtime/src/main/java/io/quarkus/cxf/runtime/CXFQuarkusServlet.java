package io.quarkus.cxf.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;

public class CXFQuarkusServlet extends CXFNonSpringServlet {

    private static final Logger LOGGER = Logger.getLogger(CXFQuarkusServlet.class);

    private static final List<WebServiceConfig> WEB_SERVICES = new ArrayList<>();

    @Override
    public void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);

        Bus bus = getBus();
        BusFactory.setDefaultBus(bus);

        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setBus(bus);

        for (WebServiceConfig config : WEB_SERVICES) {
            Object instanceService = Arc.container().instance(config.getClassName()).get();
            if (instanceService != null) {
                factory.setServiceBean(instanceService);
                factory.setAddress(config.getPath());
                factory.create();
                LOGGER.info(config.toString() + " available.");
            } else {
                LOGGER.error("Cannot initialize " + config.toString());
            }
        }
    }

    public static void publish(String path, String webService) {
        WEB_SERVICES.add(new WebServiceConfig(path, webService));
    }

    public static class WebServiceConfig {
        private String path;
        private String className;

        public WebServiceConfig(String path, String className) {
            super();
            this.path = path;
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "Web Service " + className + " on " + path;
        }

    }
}
