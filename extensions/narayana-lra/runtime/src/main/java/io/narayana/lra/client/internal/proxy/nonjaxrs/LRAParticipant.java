package io.narayana.lra.client.internal.proxy.nonjaxrs;

import static io.narayana.lra.LRAConstants.AFTER;
import static io.narayana.lra.LRAConstants.COMPENSATE;
import static io.narayana.lra.LRAConstants.COMPLETE;
import static io.narayana.lra.LRAConstants.FORGET;
import static io.narayana.lra.LRAConstants.STATUS;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.Status;

import io.narayana.lra.AnnotationResolver;
import io.narayana.lra.logging.LRALogger;

/**
 * Keeps references to individual non-JAX-RS paraticipant methods in
 * single LRA participant class.
 */
public class LRAParticipant {

    private Class<?> javaClass;
    private Method compensateMethod;
    private Method completeMethod;
    private Method statusMethod;
    private Method forgetMethod;
    private Method afterLRAMethod;
    private Object instance;

    private Map<URI, ParticipantResult> participantStatusMap = new HashMap<>();

    public LRAParticipant(Class<?> javaClass) {
        this.javaClass = javaClass;

        Arrays.stream(javaClass.getMethods()).forEach(this::processParticipantMethod);
    }

    Class<?> getJavaClass() {
        return javaClass;
    }

    synchronized Response compensate(URI lraId, URI parentId) {
        if (participantStatusMap.containsKey(lraId)) {
            processCompletionStageResult(compensateMethod, lraId, parentId, COMPENSATE);
        }

        return invokeParticipantMethod(compensateMethod, lraId, parentId, COMPENSATE);
    }

    synchronized Response complete(URI lraId, URI parentId) {
        if (participantStatusMap.containsKey(lraId)) {
            processCompletionStageResult(completeMethod, lraId, parentId, COMPLETE);
        }

        return invokeParticipantMethod(completeMethod, lraId, parentId, COMPLETE);
    }

    synchronized Response status(URI lraId, URI parentId) {
        if (participantStatusMap.containsKey(lraId)) {
            return processCompletionStageResult(statusMethod, lraId, parentId, STATUS);
        }

        return invokeParticipantMethod(statusMethod, lraId, parentId, STATUS);
    }

    synchronized Response forget(URI lraId, URI parentId) {
        return invokeParticipantMethod(forgetMethod, lraId, parentId, FORGET);
    }

    synchronized Response afterLRA(URI lraId, LRAStatus lraStatus) {
        Object result = invokeMethod(AFTER, afterLRAMethod, getInstance(), lraId, lraStatus);

        // return the result if it is a Response
        return result instanceof Response ? (Response) result : Response.ok().build();
    }

    /**
     * Process participant method for non JAX-RS related processing
     * defined by the specification and verify its signature
     *
     * @param method method to be processed
     */
    private void processParticipantMethod(Method method) {

        if (isJaxRsMethod(method)) {
            return;
        }

        if (setAfterLRAAnnotation(method) || !setParticipantAnnotation(method)) {
            return;
        }

        verifyReturnType(method);

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length > 2) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "Participant method cannot have more than 2 arguments"));
        }

        if (parameterTypes.length > 0 && !parameterTypes[0].equals(URI.class)) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "Invalid argument type in LRA participant method: " + parameterTypes[0]));
        }

        if (parameterTypes.length > 1 && !parameterTypes[1].equals(URI.class)) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "Invalid argument type in LRA participant method: " + parameterTypes[1]));
        }
    }

    private boolean setAfterLRAAnnotation(Method method) {
        if (!AnnotationResolver.isAnnotationPresent(AfterLRA.class, method)) {
            return false;
        }

        // spec defined signature is "void afterLRA(URI, LRAStatus)
        if (!method.getReturnType().equals(Void.TYPE)) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "Invalid return type for @AfterLRA method: " + method.getReturnType()));
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 2) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "@AfterLRA method cannot have more than 2 arguments"));
        }

        if (parameterTypes.length > 0 && !parameterTypes[0].equals(URI.class)) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "Invalid first argument of @AfterLRA method: " + parameterTypes[0]));
        }

        if (parameterTypes.length > 1 && !parameterTypes[1].equals(LRAStatus.class)) {
            throw new IllegalStateException(String.format("%s: %s",
                    method.toGenericString(), "Invalid second argument of @AfterLRA method: " + parameterTypes[1]));
        }

        afterLRAMethod = method;
        return true;
    }

    private boolean isJaxRsMethod(Method method) {
        return AnnotationResolver.isAnnotationPresent(GET.class, method) ||
                AnnotationResolver.isAnnotationPresent(POST.class, method) ||
                AnnotationResolver.isAnnotationPresent(PUT.class, method) ||
                AnnotationResolver.isAnnotationPresent(DELETE.class, method) ||
                AnnotationResolver.isAnnotationPresent(HEAD.class, method) ||
                AnnotationResolver.isAnnotationPresent(OPTIONS.class, method);
    }

    private boolean setParticipantAnnotation(Method method) {
        if (AnnotationResolver.isAnnotationPresent(Compensate.class, method)) {
            compensateMethod = method;
            return true;
        } else if (AnnotationResolver.isAnnotationPresent(Complete.class, method)) {
            completeMethod = method;
            return true;
        } else if (AnnotationResolver.isAnnotationPresent(Status.class, method)) {
            statusMethod = method;
            return true;
        } else if (AnnotationResolver.isAnnotationPresent(Forget.class, method)) {
            forgetMethod = method;
            return true;
        }

        return false;
    }

    private void verifyReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        if (returnType.equals(CompletionStage.class)) {
            verifyReturnType(getCompletionStageActualType(method), method.toGenericString(), true);
            return;
        }

        verifyReturnType(returnType, method.toGenericString(), false);
    }

    private void verifyReturnType(Class<?> returnType, String methodGenericString, boolean inCompletionStage) {
        if (!returnType.equals(Void.TYPE) &&
                !returnType.equals(Void.class) &&
                !returnType.equals(ParticipantStatus.class) &&
                !returnType.equals(Response.class)) {
            throw new IllegalStateException(String.format("%s: %s",
                    methodGenericString, "Invalid return type for participant method "
                            + (inCompletionStage ? "CompletionStage<" + returnType + ">" : returnType)));
        }
    }

    private Response processCompletionStageResult(Method method, URI lraId, URI parentId, String type) {
        ParticipantResult participantResult = participantStatusMap.get(lraId);
        if (participantResult.isReady()) {
            participantStatusMap.remove(lraId);

            Object result = participantResult.getValue();

            if (shouldInvokeParticipantMethod(result)) {
                LRALogger.i18nLogger.warn_participantReturnsImmediateStateFromCompletionStage(
                        getJavaClass().getName(), lraId.toASCIIString());
                return invokeParticipantMethod(method, lraId, parentId, type);
            }

            return processResult(result, participantResult.getType(), type);
        } else {
            // participant is still compensating / compeleting
            return Response.accepted().build();
        }
    }

    private boolean shouldInvokeParticipantMethod(Object result) {
        if (result instanceof ParticipantStatus) {
            ParticipantStatus participantStatus = (ParticipantStatus) result;
            if (participantStatus.equals(ParticipantStatus.Compensating) ||
                    participantStatus.equals(ParticipantStatus.Completing)) {
                return true;
            }
        } else if (result instanceof Response && ((Response) result).getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
            return true;
        }

        return false;
    }

    private Response invokeParticipantMethod(Method method, URI lraId,
            URI parentId, String type) {
        Object participant = getInstance();
        Object result;

        switch (method.getParameterCount()) {
            case 0:
                result = invokeMethod(type, method, participant);
                break;
            case 1:
                result = invokeMethod(type, method, participant, lraId);
                break;
            case 2:
                result = invokeMethod(type, method, participant, lraId, parentId);
                break;
            default:
                throw new IllegalStateException(
                        method.toGenericString() + ": invalid number of arguments: " + method.getParameterCount());
        }

        return processResult(result, lraId, method, type);
    }

    private Object getInstance() {
        return instance != null ? instance : CDI.current().select(javaClass).get();
    }

    private Object invokeMethod(String type, Method method, Object o, Object... args) {
        try {
            return method.invoke(o, args);
        } catch (InvocationTargetException e) {
            return processThrowable(e.getTargetException(), type);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Response processResult(Object result, URI lraId, Method method, String type) {
        if (result instanceof CompletionStage) {
            // store the CompletionStage result and respond compensating / completing
            participantStatusMap.put(lraId, new ParticipantResult(getCompletionStageActualType(method)));
            ((CompletionStage<?>) result)
                    .thenAccept(res -> participantStatusMap.get(lraId).setValue(res))
                    .exceptionally(throwable -> {
                        participantStatusMap.get(lraId).setValue(throwable);
                        return null;
                    });
            return Response.status(Response.Status.ACCEPTED).build();
        }

        return processResult(result, method.getReturnType(), type);
    }

    private Response processResult(Object result, Class<?> resultType, String type) {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK);

        // when the return type is void (Void.TYPE) or CompletionStage<Void> (Void.class)
        // the result is equal to null so we need to first check the result type
        if (resultType.equals(Void.TYPE) || resultType.equals(Void.class)) {
            // void return type and no exception was thrown
            builder.entity(type.equals(COMPLETE) ? ParticipantStatus.Completed.name() : ParticipantStatus.Compensated.name());
        } else if (result == null) {
            builder.status(Response.Status.NOT_FOUND);
        } else if (result instanceof ParticipantStatus) {
            ParticipantStatus status = (ParticipantStatus) result;
            if (status == ParticipantStatus.Compensating || status == ParticipantStatus.Completing) {
                return builder.status(Response.Status.ACCEPTED).build();
            } else {
                builder.entity(status.name());
            }
        } else if (result instanceof Response) {
            return (Response) result;
        } else {
            // the result must be a Throwable as no other option exists (signatures are checked during deployment)
            builder.entity(processThrowable((Throwable) result, type));
        }

        return builder.build();
    }

    private Object processThrowable(Throwable throwable, String type) {
        if (throwable instanceof WebApplicationException) {
            return ((WebApplicationException) throwable).getResponse();
        }

        // other exceptions differ based on the participant method type
        if (type.equals(COMPENSATE)) {
            LRALogger.logger.debug("Compensate participant method threw an unexpected exception", throwable);
            return ParticipantStatus.FailedToCompensate.name();
        } else if (type.equals(COMPLETE)) {
            LRALogger.logger.debug("Complete participant method threw an unexpected exception", throwable);
            return ParticipantStatus.FailedToComplete.name();
        } else {
            // @Status and @Forget should return HTTP 500
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public void augmentTerminationURIs(Map<String, String> terminateURIs, URI baseUri) {
        String baseURI = UriBuilder.fromUri(baseUri)
                .path(LRAParticipantResource.RESOURCE_PATH)
                .path(javaClass.getName())
                .build().toASCIIString();

        if (!terminateURIs.containsKey(COMPLETE) && completeMethod != null) {
            terminateURIs.put(COMPLETE, getURI(baseURI, COMPLETE));
        }

        if (!terminateURIs.containsKey(COMPENSATE) && compensateMethod != null) {
            terminateURIs.put(COMPENSATE, getURI(baseURI, COMPENSATE));
        }

        if (!terminateURIs.containsKey(STATUS) && statusMethod != null) {
            terminateURIs.put(STATUS, getURI(baseURI, STATUS));
        }

        if (!terminateURIs.containsKey(FORGET) && forgetMethod != null) {
            terminateURIs.put(FORGET, getURI(baseURI, FORGET));
        }

        if (!terminateURIs.containsKey(AFTER) && afterLRAMethod != null) {
            terminateURIs.put(AFTER, getURI(baseURI, AFTER));
        }
    }

    private String getURI(String baseURI, String path) {
        return String.format("%s/%s", baseURI, path);
    }

    boolean hasNonJaxRsMethods() {
        return compensateMethod != null ||
                completeMethod != null ||
                statusMethod != null ||
                forgetMethod != null ||
                afterLRAMethod != null;
    }

    private static Class<?> getCompletionStageActualType(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        return (Class<?>) parameterizedType.getActualTypeArguments()[0];
    }

    private static final class ParticipantResult {

        private boolean ready;
        private Class<?> type;
        private Object value;

        ParticipantResult(Class<?> type) {
            this.type = type;
        }

        boolean isReady() {
            return ready;
        }

        void setValue(Object value) {
            this.value = value;
            this.ready = true;
        }

        Class<?> getType() {
            return type;
        }

        Object getValue() {
            return value;
        }
    }
}
