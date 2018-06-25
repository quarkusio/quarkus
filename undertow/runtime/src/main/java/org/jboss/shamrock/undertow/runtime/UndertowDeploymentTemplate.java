package org.jboss.shamrock.undertow.runtime;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jboss.shamrock.codegen.ContextObject;
import org.jboss.shamrock.startup.StartupContext;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

public class UndertowDeploymentTemplate {

    @ContextObject("deploymentInfo")
    public DeploymentInfo createDeployment(String name) {
        DeploymentInfo d = new DeploymentInfo();
        d.setClassLoader(getClass().getClassLoader());
        d.setDeploymentName(name);
        d.setContextPath("/");
        return d;
    }

    public void registerServlet(@ContextObject("deploymentInfo") DeploymentInfo info, String name, String servletClass) throws Exception {
        ServletInfo servletInfo = new ServletInfo(name, (Class<? extends Servlet>) Class.forName(servletClass));
        info.addServlet(servletInfo);
    }

    public void addServletMapping(@ContextObject("deploymentInfo") DeploymentInfo info, String name, String mapping) throws Exception {
        ServletInfo sv = info.getServlets().get(name);
        sv.addMapping(mapping);
    }

    public void deploy(StartupContext startupContext, @ContextObject("deploymentInfo") DeploymentInfo info) throws ServletException {
        ServletContainer servletContainer = Servlets.defaultContainer();
        DeploymentManager manager = servletContainer.addDeployment(info);
        manager.deploy();
        Undertow val = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(manager.start())
                .build();
        val.start();
        startupContext.putValue("undertow", val);
    }

}
