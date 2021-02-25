package io.quarkus.arc.deployment.devconsole;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;

import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.processor.ObserverInfo;

public class DevObserverInfo implements Comparable<DevObserverInfo> {

    private final boolean isApplicationObserver;
    private final Name declaringClass;
    private final String methodName;
    private final Name observedType;
    private List<Name> qualifiers;
    private final int priority;
    private final boolean isAsync;
    private final Reception reception;
    private final TransactionPhase transactionPhase;

    public DevObserverInfo(ObserverInfo observer, CompletedApplicationClassPredicateBuildItem predicate) {
        priority = observer.getPriority();
        reception = observer.getReception();
        transactionPhase = observer.getTransactionPhase();
        isAsync = observer.isAsync();
        observedType = Name.from(observer.getObservedType());

        if (observer.getQualifiers().isEmpty()) {
            qualifiers = Collections.emptyList();
        } else {
            qualifiers = observer.getQualifiers().stream().map(Name::from).collect(Collectors.toList());
        }
        if (observer.getDeclaringBean() != null) {
            declaringClass = Name.from(observer.getObserverMethod().declaringClass().name());
            methodName = observer.getObserverMethod().name();
            isApplicationObserver = predicate.test(observer.getObserverMethod().declaringClass().name());
        } else {
            declaringClass = null;
            methodName = null;
            isApplicationObserver = false;
        }
    }

    public Name getDeclaringClass() {
        return declaringClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public Name getObservedType() {
        return observedType;
    }

    public List<Name> getQualifiers() {
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

    public boolean isApplicationObserver() {
        return isApplicationObserver;
    }

    @Override
    public int compareTo(DevObserverInfo o) {
        // Application beans should go first
        if (isApplicationObserver == o.isApplicationObserver) {
            if (declaringClass == null && o.declaringClass != null) {
                return -1;
            } else if (declaringClass != null && o.declaringClass == null) {
                return 1;
            }
            int ret = declaringClass.compareTo(o.declaringClass);
            return ret == 0 ? methodName.compareTo(o.methodName) : ret;
        }
        return isApplicationObserver ? -1 : 1;
    }
}
