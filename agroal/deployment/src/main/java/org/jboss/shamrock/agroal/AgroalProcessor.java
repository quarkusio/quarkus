package org.jboss.shamrock.agroal;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import org.jboss.shamrock.agroal.runtime.DataSourceProducer;
import org.jboss.shamrock.agroal.runtime.DataSourceTemplate;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.runtime.ConfiguredValue;

class AgroalProcessor {


    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(DataSourceProducer.class);
    }

    @Record(STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(BuildConfig config,
                                         BuildProducer<ReflectiveClassBuildItem> reflectiveClass, DataSourceTemplate template, RecorderContext bc) throws Exception {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()
        ));
        BuildConfig.ConfigNode ds = config.getApplicationConfig().get("datasource");
        if (ds.isNull()) {
            return null;
        }
        String driver = ds.get("driver").asString();
        String url = ds.get("url").asString();
        ConfiguredValue configuredDriver = new ConfiguredValue("datasource.driver", driver);
        ConfiguredValue configuredURL = new ConfiguredValue("datasource.url", url);
        if (configuredDriver.getValue() == null) {
            throw new RuntimeException("Driver is required (property 'driver' under 'datasource')");
        }
        if (configuredURL.getValue() == null) {
            throw new RuntimeException("JDBC URL is required (property 'url' under 'datasource')");
        }
        String userName = ds.get("username").asString();
        ConfiguredValue configuredUsername = new ConfiguredValue("datasource.user", userName);
        String password = ds.get("password").asString();
        ConfiguredValue configuredPassword = new ConfiguredValue("datasource.password", password);

        final Integer minSize = ds.get("minSize").asInteger();
        final Integer maxSize = ds.get("maxSize").asInteger();


        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driver));

        return new BeanContainerListenerBuildItem(template.addDatasource(configuredURL.getValue(), bc.classProxy(configuredDriver.getValue()), configuredUsername.getValue(), configuredPassword.getValue(), minSize, maxSize));

    }
}
