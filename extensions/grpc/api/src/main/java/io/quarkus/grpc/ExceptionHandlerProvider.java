package io.quarkus.grpc;

import java.util.Optional;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Provider for ExceptionHandler. To use a custom ExceptionHandler, extend {@link ExceptionHandler} and implement an
 * {@link ExceptionHandlerProvider}, and expose it as a CDI bean.
 */
public interface ExceptionHandlerProvider {
    <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata);

    default Throwable transform(Throwable t) {
        return toStatusException(t, false); // previous default was false
    }

    /**
     * Throw Status exception.
     *
     * @param t
     *        the throwable to transform
     * @param runtime
     *        true if we should throw StatusRuntimeException, false for StatusException
     *
     * @return Status(Runtime)Exception
     */
    static Exception toStatusException(Throwable t, boolean runtime) {
        if (t instanceof StatusException || t instanceof StatusRuntimeException) {
            if (runtime) {
                if (t instanceof StatusRuntimeException) {
                    return (Exception) t;
                } else {
                    StatusException se = (StatusException) t;
                    return new StatusRuntimeException(se.getStatus(), se.getTrailers());
                }
            } else {
                if (t instanceof StatusException) {
                    return (Exception) t;
                } else {
                    StatusRuntimeException sre = (StatusRuntimeException) t;
                    return new StatusException(sre.getStatus(), sre.getTrailers());
                }
            }
        } else {
            String desc = t.getClass().getName();
            if (t.getMessage() != null) {
                desc += " - " + t.getMessage();
            }
            Status status;
            if (t instanceof IllegalArgumentException) {
                status = Status.INVALID_ARGUMENT.withDescription(desc);
            } else {
                status = Status.fromThrowable(t).withDescription(desc);
            }
            return runtime ? status.asRuntimeException() : status.asException();
        }
    }

    /**
     * Get Status from exception.
     *
     * @param t
     *        the throwable to read or create status from
     *
     * @return gRPC Status instance
     */
    static Status toStatus(Throwable t) {
        if (t instanceof StatusException) {
            return ((StatusException) t).getStatus();
        } else if (t instanceof StatusRuntimeException) {
            return ((StatusRuntimeException) t).getStatus();
        } else {
            String desc = t.getClass().getName();
            if (t.getMessage() != null) {
                desc += " - " + t.getMessage();
            }
            if (t instanceof IllegalArgumentException) {
                return Status.INVALID_ARGUMENT.withDescription(desc);
            }
            return Status.fromThrowable(t).withDescription(desc);
        }
    }

    /**
     * Get optional Metadata from exception.
     *
     * @param t
     *        the throwable to read or create metadata from
     *
     * @return optional gRPC Metadata instance
     */
    static Optional<Metadata> toTrailers(Throwable t) {
        Metadata trailers = null;
        if (t instanceof StatusException) {
            trailers = ((StatusException) t).getTrailers();
        } else if (t instanceof StatusRuntimeException) {
            trailers = ((StatusRuntimeException) t).getTrailers();
        }
        return Optional.ofNullable(trailers);
    }
}
