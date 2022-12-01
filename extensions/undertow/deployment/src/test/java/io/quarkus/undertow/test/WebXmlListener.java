package io.quarkus.undertow.test;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WebXmlListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("web xml listener", true);
    }
}
