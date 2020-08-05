package io.quarkus.jdbc.mysql.deployment;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mysql.cj.MysqlConnection;
import com.mysql.cj.WarningListener;
import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.jdbc.JdbcConnection;
import com.mysql.cj.jdbc.JdbcPreparedStatement;
import com.mysql.cj.jdbc.JdbcPropertySet;
import com.mysql.cj.jdbc.JdbcStatement;
import com.mysql.cj.jdbc.ha.LoadBalancedConnection;
import com.mysql.cj.jdbc.ha.ReplicationConnection;
import com.mysql.cj.jdbc.result.ResultSetInternalMethods;
import com.mysql.cj.protocol.Resultset;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllTimeZonesBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.jdbc.mysql.runtime.MySQLAgroalConnectionConfigurer;
import io.quarkus.jdbc.mysql.runtime.MySQLRecorder;

public class JDBCMySQLProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_MYSQL);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.MYSQL, "com.mysql.cj.jdbc.Driver",
                "com.mysql.cj.jdbc.MysqlXADataSource"));
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClass(MySQLAgroalConnectionConfigurer.class)
                    .setDefaultScope(BuiltinScope.APPLICATION.getName())
                    .setUnremovable()
                    .build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void abandonedConnectionCleanUp(MySQLRecorder recorder) {
        recorder.startAbandonedConnectionCleanup();
    }

    @BuildStep
    NativeImageResourceBuildItem resource() {
        return new NativeImageResourceBuildItem("com/mysql/cj/util/TimeZoneMapping.properties");
    }

    @BuildStep
    NativeImageEnableAllCharsetsBuildItem enableAllCharsets() {
        return new NativeImageEnableAllCharsetsBuildItem();
    }

    @BuildStep
    NativeImageEnableAllTimeZonesBuildItem enableAllTimeZones() {
        return new NativeImageEnableAllTimeZonesBuildItem();
    }

    @BuildStep
    List<NativeImageProxyDefinitionBuildItem> registerProxies() {
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();
        proxies.add(new NativeImageProxyDefinitionBuildItem(JdbcConnection.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(MysqlConnection.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(Statement.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(AutoCloseable.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(JdbcStatement.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(Connection.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(ResultSet.class.getName()));
        proxies.add(
                new NativeImageProxyDefinitionBuildItem(JdbcPreparedStatement.class.getName(), JdbcStatement.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(JdbcPropertySet.class.getName(), PropertySet.class.getName(),
                Serializable.class.getName()));
        proxies.add(
                new NativeImageProxyDefinitionBuildItem(Resultset.class.getName(), ResultSetInternalMethods.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(LoadBalancedConnection.class.getName(),
                JdbcConnection.class.getName()));
        proxies.add(
                new NativeImageProxyDefinitionBuildItem(ReplicationConnection.class.getName(), JdbcConnection.class.getName()));
        proxies.add(
                new NativeImageProxyDefinitionBuildItem(ResultSetInternalMethods.class.getName(),
                        WarningListener.class.getName(), Resultset.class.getName()));
        return proxies;
    }
}
