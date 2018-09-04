package org.jboss.shamrock.agroal;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Function;

import javax.inject.Inject;

import org.jboss.shamrock.agroal.runtime.DataSourceProducer;
import org.jboss.shamrock.agroal.runtime.DataSourceTemplate;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.agroal.pool.ConnectionHandler;

class AgroalProcessor implements ResourceProcessor {

    @Inject
    private BeanDeployment beanDeployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        BuildConfig config = archiveContext.getBuildConfig();
        BuildConfig.ConfigNode ds = config.getApplicationConfig().get("datasource");
        if (ds.isNull()) {
            return;
        }
        String driver = ds.get("driver").asString();
        String url = ds.get("url").asString();
        if (driver == null) {
            throw new RuntimeException("Driver is required");
        }
        if (url == null) {
            throw new RuntimeException("Driver is required");
        }
        String userName = ds.get("username").asString();
        String password = ds.get("password").asString();

        processorContext.addReflectiveClass(false, false, driver);
        beanDeployment.addAdditionalBean(DataSourceProducer.class);
        try (BytecodeRecorder bc = processorContext.addDeploymentTask(RuntimePriority.DATASOURCE_DEPLOYMENT)) {
            DataSourceTemplate template = bc.getRecordingProxy(DataSourceTemplate.class);
            template.addDatasource(null, url, bc.classProxy(driver), userName, password);
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
