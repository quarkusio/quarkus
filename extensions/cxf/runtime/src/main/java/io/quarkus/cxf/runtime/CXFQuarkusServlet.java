package io.quarkus.cxf.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Arc;

public class CXFQuarkusServlet extends CXFNonSpringServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CXFQuarkusServlet.class);

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

    private static final long serialVersionUID = 1L;

    private static final List<WebServiceConfig> WEB_SERVICES = new ArrayList<>();

    @Override
    public void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);

        // You could add the endpoint publish codes here
        Bus bus = getBus();
        BusFactory.setDefaultBus(bus);

        // You can als use the simple frontend API to do this
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
}
