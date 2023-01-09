package io.quarkus.arc.deployment.devconsole;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;

import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.processor.ObserverInfo;

public class DevObserverInfo implements Comparable<DevObserverInfo> {

    public static DevObserverInfo from(ObserverInfo observer, CompletedApplicationClassPredicateBuildItem predicate) {
        List<Name> qualifiers;
        if (observer.getQualifiers().isEmpty()) {
            qualifiers = Collections.emptyList();
        } else {
            qualifiers = observer.getQualifiers().stream().map(Name::from).collect(Collectors.toList());
        }
        if (observer.getDeclaringBean() != null) {
            return new DevObserverInfo(predicate.test(observer.getObserverMethod().declaringClass().name()),
                    Name.from(observer.getObserverMethod().declaringClass().name()), observer.getObserverMethod().name(),
                    Name.from(observer.getObservedType()), qualifiers, observer.getPriority(), observer.isAsync(),
                    observer.getReception(), observer.getTransactionPhase());
        } else {
            return new DevObserverInfo(false, null, null, Name.from(observer.getObservedType()), qualifiers,
                    observer.getPriority(), observer.isAsync(), observer.getReception(), observer.getTransactionPhase());
        }
    }

    private final boolean isApplicationObserver;
    private final Name declaringClass;
    private final String methodName;
    private final Name observedType;
    private List<Name> qualifiers;
    private final int priority;
    private final boolean isAsync;
    private final Reception reception;
    private final TransactionPhase transactionPhase;

    public DevObserverInfo(boolean isApplicationObserver, Name declaringClass, String methodName, Name observedType,
            List<Name> qualifiers, int priority, boolean isAsync, Reception reception, TransactionPhase transactionPhase) {
        this.isApplicationObserver = isApplicationObserver;
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.observedType = observedType;
        this.qualifiers = qualifiers;
        this.priority = priority;
        this.isAsync = isAsync;
        this.reception = reception;
        this.transactionPhase = transactionPhase;
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
    public int compareTo(DevObserverInfo other) {
        // Application observers should go first
        int ret = 0;
        if (isApplicationObserver != other.isApplicationObserver) {
            ret = isApplicationObserver ? -1 : 1;
        } else {
            // Note that declaringClass and methodName are null for synthetic observers
            if (declaringClass == null && other.declaringClass == null) {
                // Synthetic observers
                ret = observedType.compareTo(other.observedType);
            } else {
                // At this point we can be sure that at least one of the observers is not synthetic
                if (declaringClass == null && other.declaringClass != null) {
                    ret = 1;
                } else if (declaringClass != null && other.declaringClass == null) {
                    ret = -1;
                } else {
                    ret = declaringClass.compareTo(other.declaringClass);
                }
                if (ret == 0) {
                    // Observers are not synthetic - method name must be present
                    ret = methodName.compareTo(other.methodName);
                }
            }
        }
        return ret;
    }
}
