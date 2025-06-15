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

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Driver.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(FailoverDnsSrvConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(FailoverConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SingleConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(LoadBalanceConnectionUrl.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(LoadBalanceDnsSrvConnectionUrl.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(ReplicationDnsSrvConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ReplicationConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(XDevApiConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(XDevApiDnsSrvConnectionUrl.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(com.mysql.cj.jdbc.ha.LoadBalancedAutoCommitInterceptor.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(StandardLogger.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Wrapper.class.getName()).build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(com.mysql.cj.jdbc.MysqlDataSource.class.getName()).methods().build());
    }

    @BuildStep
    void registerSocketFactoryForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(NamedPipeSocketFactory.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(StandardSocketFactory.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SocksProxySocketFactory.class.getName()).build());
    }

    @BuildStep
    void registerExceptionsForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CJCommunicationsException.class.getName()).build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(CJConnectionFeatureNotAvailableException.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(CJOperationNotSupportedException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CJTimeoutException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CJPacketTooBigException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CJException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(AssertionFailedException.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(CJOperationNotSupportedException.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(ClosedOnExpiredPasswordException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ConnectionIsClosedException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(DataConversionException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(DataReadException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(DataTruncationException.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(DeadlockTimeoutRollbackMarker.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(FeatureNotAvailableException.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(InvalidConnectionAttributeException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(NumberOutOfRange.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(OperationCancelledException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(PasswordExpiredException.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(PropertyNotModifiableException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(RSAException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SSLParamsException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(StatementIsClosedException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(StreamingNotifiable.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(UnableToConnectException.class.getName()).build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(UnsupportedConnectionStringException.class.getName()).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(WrongArgumentException.class.getName()).build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("com.mysql.cj.jdbc.MysqlXAException").methods().fields().build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(StandardLoadBalanceExceptionChecker.class.getName()).build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(NdbLoadBalanceExceptionChecker.class.getName()).build());
    }
}
