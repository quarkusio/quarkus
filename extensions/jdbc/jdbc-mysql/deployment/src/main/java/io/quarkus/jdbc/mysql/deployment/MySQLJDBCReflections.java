package io.quarkus.jdbc.mysql.deployment;

import java.sql.Wrapper;

import com.mysql.cj.conf.url.FailoverConnectionUrl;
import com.mysql.cj.conf.url.FailoverDnsSrvConnectionUrl;
import com.mysql.cj.conf.url.LoadBalanceConnectionUrl;
import com.mysql.cj.conf.url.LoadBalanceDnsSrvConnectionUrl;
import com.mysql.cj.conf.url.ReplicationConnectionUrl;
import com.mysql.cj.conf.url.ReplicationDnsSrvConnectionUrl;
import com.mysql.cj.conf.url.SingleConnectionUrl;
import com.mysql.cj.conf.url.XDevApiConnectionUrl;
import com.mysql.cj.conf.url.XDevApiDnsSrvConnectionUrl;
import com.mysql.cj.exceptions.AssertionFailedException;
import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.exceptions.CJConnectionFeatureNotAvailableException;
import com.mysql.cj.exceptions.CJException;
import com.mysql.cj.exceptions.CJOperationNotSupportedException;
import com.mysql.cj.exceptions.CJPacketTooBigException;
import com.mysql.cj.exceptions.CJTimeoutException;
import com.mysql.cj.exceptions.ClosedOnExpiredPasswordException;
import com.mysql.cj.exceptions.ConnectionIsClosedException;
import com.mysql.cj.exceptions.DataConversionException;
import com.mysql.cj.exceptions.DataReadException;
import com.mysql.cj.exceptions.DataTruncationException;
import com.mysql.cj.exceptions.DeadlockTimeoutRollbackMarker;
import com.mysql.cj.exceptions.FeatureNotAvailableException;
import com.mysql.cj.exceptions.InvalidConnectionAttributeException;
import com.mysql.cj.exceptions.NumberOutOfRange;
import com.mysql.cj.exceptions.OperationCancelledException;
import com.mysql.cj.exceptions.PasswordExpiredException;
import com.mysql.cj.exceptions.PropertyNotModifiableException;
import com.mysql.cj.exceptions.RSAException;
import com.mysql.cj.exceptions.SSLParamsException;
import com.mysql.cj.exceptions.StatementIsClosedException;
import com.mysql.cj.exceptions.StreamingNotifiable;
import com.mysql.cj.exceptions.UnableToConnectException;
import com.mysql.cj.exceptions.UnsupportedConnectionStringException;
import com.mysql.cj.exceptions.WrongArgumentException;
import com.mysql.cj.jdbc.Driver;
import com.mysql.cj.jdbc.ha.NdbLoadBalanceExceptionChecker;
import com.mysql.cj.jdbc.ha.StandardLoadBalanceExceptionChecker;
import com.mysql.cj.log.StandardLogger;
import com.mysql.cj.protocol.NamedPipeSocketFactory;
import com.mysql.cj.protocol.SocksProxySocketFactory;
import com.mysql.cj.protocol.StandardSocketFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public final class MySQLJDBCReflections {

    @BuildStep
    void registerDriverForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(Driver.class.getName(), FailoverDnsSrvConnectionUrl.class.getName(),
                        FailoverConnectionUrl.class.getName(), SingleConnectionUrl.class.getName(),
                        LoadBalanceConnectionUrl.class.getName(), LoadBalanceDnsSrvConnectionUrl.class.getName(),
                        ReplicationDnsSrvConnectionUrl.class.getName(), ReplicationConnectionUrl.class.getName(),
                        XDevApiConnectionUrl.class.getName(), XDevApiDnsSrvConnectionUrl.class.getName(),
                        com.mysql.cj.jdbc.ha.LoadBalancedAutoCommitInterceptor.class.getName(), StandardLogger.class.getName(),
                        Wrapper.class.getName()).build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(com.mysql.cj.jdbc.MysqlDataSource.class.getName())
                .methods().build());
    }

    @BuildStep
    void registerSocketFactoryForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                NamedPipeSocketFactory.class.getName(),
                StandardSocketFactory.class.getName(),
                SocksProxySocketFactory.class.getName()).build());
    }

    @BuildStep
    void registerExceptionsForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                CJCommunicationsException.class.getName(),
                CJConnectionFeatureNotAvailableException.class.getName(),
                CJOperationNotSupportedException.class.getName(),
                CJTimeoutException.class.getName(),
                CJPacketTooBigException.class.getName(),
                CJException.class.getName(),
                AssertionFailedException.class.getName(),
                CJOperationNotSupportedException.class.getName(),
                ClosedOnExpiredPasswordException.class.getName(),
                ConnectionIsClosedException.class.getName(),
                DataConversionException.class.getName(),
                DataReadException.class.getName(),
                DataTruncationException.class.getName(),
                DeadlockTimeoutRollbackMarker.class.getName(),
                FeatureNotAvailableException.class.getName(),
                InvalidConnectionAttributeException.class.getName(),
                NumberOutOfRange.class.getName(),
                OperationCancelledException.class.getName(),
                PasswordExpiredException.class.getName(),
                PropertyNotModifiableException.class.getName(),
                RSAException.class.getName(),
                SSLParamsException.class.getName(),
                StatementIsClosedException.class.getName(),
                StreamingNotifiable.class.getName(),
                UnableToConnectException.class.getName(),
                UnsupportedConnectionStringException.class.getName(),
                WrongArgumentException.class.getName(),
                StandardLoadBalanceExceptionChecker.class.getName(),
                NdbLoadBalanceExceptionChecker.class.getName())
                .build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("com.mysql.cj.jdbc.MysqlXAException").methods().fields().build());
    }
}
