package io.quarkus.arc.deployment.devui;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.processor.DecoratorInfo;

public class DevDecoratorInfo implements Comparable<DevDecoratorInfo> {

    public static DevDecoratorInfo from(DecoratorInfo decorator,
            CompletedApplicationClassPredicateBuildItem predicate) {
        boolean isApplicationBean = predicate.test(decorator.getBeanClass());
        Set<Name> delegateQualifiers = new HashSet<>();
        for (AnnotationInstance binding : decorator.getDelegateQualifiers()) {
            delegateQualifiers.add(Name.from(binding));
        }
        return new DevDecoratorInfo(decorator.getIdentifier(), Name.from(decorator.getBeanClass()),
                Name.from(decorator.getDelegateType()), delegateQualifiers, decorator.getPriority(), isApplicationBean);
    }

    private final String id;
    private final Name decoratorClass;
    private final Name delegateType;
    private final Set<Name> delegateQualifiers;
    private final int priority;
    private final boolean isApplicationBean;

    private DevDecoratorInfo(String id, Name decoratorClass, Name delegateType, Set<Name> delegateQualifiers,
            int priority, boolean isApplicationBean) {
        super();
        this.id = id;
        this.decoratorClass = decoratorClass;
        this.delegateType = delegateType;
        this.delegateQualifiers = delegateQualifiers;
        this.priority = priority;
        this.isApplicationBean = isApplicationBean;
    }

    public String getId() {
        return id;
    }

    public Name getDecoratorClass() {
        return decoratorClass;
    }

    public Name getDelegateType() {
        return delegateType;
    }

    public Set<Name> getDelegateQualifiers() {
        return delegateQualifiers;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isApplicationBean() {
        return isApplicationBean;
    }

    @Override
    public int compareTo(DevDecoratorInfo o) {
        // Application beans should go first
        if (isApplicationBean == o.isApplicationBean) {
            return decoratorClass.compareTo(o.decoratorClass);
        }
        return isApplicationBean ? -1 : 1;
    }

}
