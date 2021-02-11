package io.quarkus.arc.deployment.devconsole;

import java.util.List;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;

public class DevObserverInfo implements Comparable<DevObserverInfo> {

    private final ClassName name;
    private final String methodName;
    private final String observedType;
    private List<ClassName> qualifiers;
    private final int priority;
    private final boolean isAsync;
    private final Reception reception;
    private final TransactionPhase transactionPhase;

    public DevObserverInfo(ClassName name, String methodInfo, String observedType, List<ClassName> qualifiers, int priority,
            boolean isAsync, Reception reception, TransactionPhase transactionPhase) {
        this.name = name;
        this.methodName = methodInfo;
        this.observedType = observedType;
        this.qualifiers = qualifiers;
        this.priority = priority;
        this.isAsync = isAsync;
        this.reception = reception;
        this.transactionPhase = transactionPhase;
    }

    public ClassName getName() {
        return name;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getObservedType() {
        return observedType;
    }

    public List<ClassName> getQualifiers() {
        return qualifiers;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public Reception getReception() {
        return reception;
    }

    public TransactionPhase getTransactionPhase() {
        return transactionPhase;
    }

    @Override
    public int compareTo(DevObserverInfo o) {
        if (name == null && o.name != null) {
            return -1;
        } else if (name != null && o.name == null) {
            return 1;
        }
        int ret = name.getLocalName().compareTo(o.name.getLocalName());
        return ret == 0 ? methodName.compareTo(o.methodName) : ret;
    }
}
