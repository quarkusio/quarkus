package org.jboss.shamrock.agroal;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.shamrock.agroal.runtime.DataSourceConfig;
import org.jboss.shamrock.agroal.runtime.DataSourceProducer;
import org.jboss.shamrock.agroal.runtime.DataSourceTemplate;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanContainerListenerBuildItem;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    /**
     * The datasource configuration
     */
    @ConfigProperty(name = "shamrock.datasource")
    Optional<DataSourceConfig> dataSourceConfig;


    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(DataSourceProducer.class);
    }

    @Record(STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass, DataSourceTemplate template) throws Exception {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()
        ));
        if (!dataSourceConfig.isPresent()) {
            log.warn("Agroal extension was included in build however no data source has been defined");
            return null;
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, dataSourceConfig.get().driver));
        return new BeanContainerListenerBuildItem(template.addDatasource(dataSourceConfig.get()));

    }

}
