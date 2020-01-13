package io.quarkus.jdbc.mysql.deployment;

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
import com.mysql.cj.protocol.AsyncSocketFactory;
import com.mysql.cj.protocol.NamedPipeSocketFactory;
import com.mysql.cj.protocol.SocksProxySocketFactory;
import com.mysql.cj.protocol.StandardSocketFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public final class MySQLJDBCReflections {

    @BuildStep
    void registerDriverForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, com.mysql.cj.jdbc.Driver.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.FailoverDnsSrvConnectionUrl.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.FailoverConnectionUrl.class.getName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.SingleConnectionUrl.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.LoadBalanceConnectionUrl.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                com.mysql.cj.conf.url.LoadBalanceDnsSrvConnectionUrl.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                com.mysql.cj.conf.url.ReplicationDnsSrvConnectionUrl.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.ReplicationConnectionUrl.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.XDevApiConnectionUrl.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, com.mysql.cj.conf.url.XDevApiDnsSrvConnectionUrl.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                com.mysql.cj.jdbc.ha.LoadBalancedAutoCommitInterceptor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, com.mysql.cj.log.StandardLogger.class.getName()));
    }

    @BuildStep
    void registerSocketFactoryForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, NamedPipeSocketFactory.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StandardSocketFactory.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, SocksProxySocketFactory.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, AsyncSocketFactory.class.getName()));
    }

    @BuildStep
    void registerExceptionsForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CJCommunicationsException.class.getName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, CJConnectionFeatureNotAvailableException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CJOperationNotSupportedException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CJTimeoutException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CJPacketTooBigException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CJException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, AssertionFailedException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CJOperationNotSupportedException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ClosedOnExpiredPasswordException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ConnectionIsClosedException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DataConversionException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DataReadException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DataTruncationException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DeadlockTimeoutRollbackMarker.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, FeatureNotAvailableException.class.getName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, InvalidConnectionAttributeException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, NumberOutOfRange.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, OperationCancelledException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, PasswordExpiredException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, PropertyNotModifiableException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RSAException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, SSLParamsException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StatementIsClosedException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StreamingNotifiable.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, UnableToConnectException.class.getName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, UnsupportedConnectionStringException.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, WrongArgumentException.class.getName()));
    }
}
