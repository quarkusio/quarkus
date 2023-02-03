package io.quarkus.undertow.test;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class WebXmlListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("web xml listener", true);
    }
}
