package io.quarkus.grpc;

import static io.quarkus.grpc.ExceptionCauseSupport.encodeCauses;
import static io.quarkus.grpc.ExceptionCauseSupport.restoreCauses;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class ExceptionCauseSupportTest {

    @Test
    void shouldAttachCausesFromMetadata() {
        Metadata trailers = new Metadata();
        encodeCauses(new RuntimeException("top", new IllegalStateException("middle")), trailers);

        StatusRuntimeException exception = Status.UNKNOWN.withDescription("top").asRuntimeException(trailers);
        Throwable restored = restoreCauses(exception);

        assertThat(restored).isInstanceOf(StatusRuntimeException.class);
        assertThat(restored.getCause()).isInstanceOf(GrpcRemoteException.class);
        assertThat(restored.getCause()).hasMessage("middle");
    }

    @Test
    void shouldEncodeAndDecodeCauseChain() {
        Throwable root = new RuntimeException("outer",
                new IllegalStateException("middle", new IllegalArgumentException("inner")));

        String encoded = ExceptionCauseSupport.encodeCauseChain(root.getCause());
        Throwable decoded = ExceptionCauseSupport.decodeCauseChain(encoded);

        assertThat(decoded).isInstanceOf(GrpcRemoteException.class);
        assertThat(((GrpcRemoteException) decoded).getExceptionClassName())
                .isEqualTo(IllegalStateException.class.getName());
        assertThat(decoded).hasMessage("middle");
        assertThat(decoded.getCause()).isInstanceOf(GrpcRemoteException.class);
        assertThat(((GrpcRemoteException) decoded.getCause()).getExceptionClassName())
                .isEqualTo(IllegalArgumentException.class.getName());
        assertThat(decoded.getCause()).hasMessage("inner");
    }

    @Test
    void shouldEscapeSpecialCharacters() {
        Throwable cause = new RuntimeException("line1\\line2|:");

        String encoded = ExceptionCauseSupport.encodeCauseChain(cause);
        Throwable decoded = ExceptionCauseSupport.decodeCauseChain(encoded);

        assertThat(decoded).isInstanceOf(GrpcRemoteException.class);
        assertThat(decoded).hasMessage("line1\\line2|:");
    }
}
