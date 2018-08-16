package org.jboss.shamrock.health;

import javax.inject.Inject;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.health.runtime.HealthServlet;
import org.jboss.shamrock.undertow.ServletData;
import org.jboss.shamrock.undertow.ServletDeployment;
import org.jboss.shamrock.weld.deployment.WeldDeployment;

class HealthProcessor implements ResourceProcessor {

    @Inject
    private WeldDeployment weldDeployment;

    @Inject
    private ShamrockConfig config;

    @Inject
    private ServletDeployment servletDeployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        ServletData servletData = new ServletData("health", HealthServlet.class.getName());
        servletData.getMapings().add(config.getConfig("health.path", "/health"));
        servletDeployment.addServlet(servletData);
        weldDeployment.addAdditionalBean(HealthServlet.class);
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
