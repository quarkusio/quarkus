package io.quarkus.signals.deployment;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.InvokerInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.signals.spi.Receiver.ExecutionModel;

final class ReceiverMethodBuildItem extends MultiBuildItem implements Comparable<ReceiverMethodBuildItem> {

    private final ExecutionModel executionModel;
    private final BeanInfo bean;
    private final InvokerInfo invoker;
    private final MethodInfo method;
    private final MethodParameterInfo signalParam;
    private final List<AnnotationInstance> qualifiers;

    ReceiverMethodBuildItem(ExecutionModel executionModel, BeanInfo bean,
            InvokerInfo invoker, MethodInfo method, MethodParameterInfo signalParam, List<AnnotationInstance> qualifiers) {
        this.executionModel = executionModel;
        this.bean = bean;
        this.invoker = invoker;
        this.method = method;
        this.signalParam = signalParam;
        this.qualifiers = qualifiers;
    }

    public ExecutionModel getExecutionModel() {
        return executionModel;
    }

    public BeanInfo getBean() {
        return bean;
    }

    public InvokerInfo getInvoker() {
        return invoker;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public MethodParameterInfo getSignalParam() {
        return signalParam;
    }

    public Type getSignalType() {
        Type paramType = signalParam.type();
        if (paramType.kind() == Type.Kind.PARAMETERIZED_TYPE
                && paramType.asParameterizedType().name().equals(DotNames.SIGNAL_CONTEXT)) {
            // Unwrap SignalContext<T> to T
            return paramType.asParameterizedType().arguments().get(0);
        }
        return paramType;
    }

    public Type getResponseType() {
        Type returnType = method.returnType();
        if (returnType.kind() == Kind.VOID) {
            return null;
        } else if (returnType.name().equals(DotNames.UNI) || returnType.name().equals(DotNames.COMPLETION_STAGE)) {
            return returnType.asParameterizedType().arguments().get(0);
        } else {
            return returnType;
        }
    }

    public List<AnnotationInstance> getQualifiers() {
        return qualifiers;
    }

    @Override
    public int compareTo(ReceiverMethodBuildItem other) {
        int cmp = method.declaringClass().name().compareTo(other.method.declaringClass().name());
        if (cmp == 0) {
            cmp = method.toString().compareTo(other.method.toString());
        }
        return cmp;
    }

}
