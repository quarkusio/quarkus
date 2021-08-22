package io.quarkus.smallrye.reactivemessaging.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;

import io.quarkus.arc.Arc;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.MediatorConfiguration;
import io.smallrye.reactive.messaging.Shape;
import io.smallrye.reactive.messaging.annotations.Merge;

public class QuarkusMediatorConfiguration implements MediatorConfiguration {

    private String beanId;

    private String methodName;

    private Class<?> returnType;

    private Class<?>[] parameterTypes;

    private Shape shape;

    private List<String> incomings = new ArrayList<>();

    private String outgoing;

    private Acknowledgment.Strategy acknowledgment;

    private Integer broadcastValue;

    private MediatorConfiguration.Production production = MediatorConfiguration.Production.NONE;

    private MediatorConfiguration.Consumption consumption = MediatorConfiguration.Consumption.NONE;

    private boolean useBuilderTypes = false;

    private Merge.Mode merge;

    private Class<? extends Invoker> invokerClass;

    private boolean blocking;

    private boolean blockingExecutionOrdered;

    private String workerPoolName;

    private Type ingestedPayload;

    public String getBeanId() {
        return beanId;
    }

    public void setBeanId(String beanId) {
        this.beanId = beanId;
    }

    @Override
    public Bean<?> getBean() {
        return Arc.container().bean(beanId);
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public Shape shape() {
        return shape;
    }

    @Override
    public List<String> getIncoming() {
        return incomings;
    }

    public void setIncomings(List<String> incomings) {
        this.incomings = incomings;
    }

    @Override
    public String getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(String outgoing) {
        this.outgoing = outgoing;
    }

    @Override
    public Acknowledgment.Strategy getAcknowledgment() {
        return acknowledgment;
    }

    public void setAcknowledgment(Acknowledgment.Strategy acknowledgment) {
        this.acknowledgment = acknowledgment;
    }

    public Integer getBroadcastValue() {
        return broadcastValue;
    }

    public void setBroadcastValue(Integer broadcastValue) {
        this.broadcastValue = broadcastValue;
    }

    @Override
    public boolean getBroadcast() {
        return broadcastValue != null;
    }

    public MediatorConfiguration.Production getProduction() {
        return production;
    }

    public void setProduction(MediatorConfiguration.Production production) {
        this.production = production;
    }

    @Override
    public MediatorConfiguration.Production production() {
        return production;
    }

    public MediatorConfiguration.Consumption getConsumption() {
        return consumption;
    }

    public void setConsumption(MediatorConfiguration.Consumption consumption) {
        this.consumption = consumption;
    }

    @Override
    public MediatorConfiguration.Consumption consumption() {
        return consumption;
    }

    public boolean isUseBuilderTypes() {
        return useBuilderTypes;
    }

    public void setUseBuilderTypes(boolean useBuilderTypes) {
        this.useBuilderTypes = useBuilderTypes;
    }

    @Override
    public boolean usesBuilderTypes() {
        return useBuilderTypes;
    }

    public Merge.Mode getMerge() {
        return merge;
    }

    public void setMerge(Merge.Mode merge) {
        this.merge = merge;
    }

    @Override
    public Class<? extends Invoker> getInvokerClass() {
        return invokerClass;
    }

    @Override
    public Type getIngestedPayloadType() {
        return ingestedPayload;
    }

    public void setIngestedPayloadType(Type ingestedPayload) {
        this.ingestedPayload = ingestedPayload;
    }

    public void setInvokerClass(Class<? extends Invoker> invokerClass) {
        this.invokerClass = invokerClass;
    }

    @Override
    public String methodAsString() {
        if (Arc.container() != null && getBean() != null) {
            return getBean().getBeanClass().getName() + "#" + getMethodName();
        } else {
            return getMethodName();
        }

    }

    @Override
    public Method getMethod() {
        throw new UnsupportedOperationException("getMethod is not meant to be called on " + this.getClass().getName());
    }

    @Override
    public int getNumberOfSubscriberBeforeConnecting() {
        if (!getBroadcast()) {
            return -1;
        } else {
            return broadcastValue;
        }
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public String getWorkerPoolName() {
        return workerPoolName;
    }

    @Override
    public boolean isBlockingExecutionOrdered() {
        return blockingExecutionOrdered;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public void setBlockingExecutionOrdered(boolean blockingExecutionOrdered) {
        this.blockingExecutionOrdered = blockingExecutionOrdered;
    }

    public void setWorkerPoolName(String workerPoolName) {
        this.workerPoolName = workerPoolName;
    }
}
