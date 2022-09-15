package io.quarkus.arc.processor;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import java.util.Collection;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Allows a build-time extension to:
 * <ul>
 * <li>override the set of qualifiers and priority of any observer method,</li>
 * <li>instruct the container to veto the observer method.</li>
 * </ul>
 */
public interface ObserverTransformer extends BuildExtension {

    /**
     *
     * @param observedType
     * @param qualifiers
     * @return {@code true} if this transformer is meant to be applied to the supplied attributes of an observer method,
     *         {@code false} otherwise
     */
    boolean appliesTo(Type observedType, Set<AnnotationInstance> qualifiers);

    /**
     * @param context
     */
    void transform(TransformationContext context);

    /**
     * Transformation context.
     */
    interface TransformationContext extends BuildExtension.BuildContext {

        /**
         *
         * @return the corresponding observer method or null in case of synthetic observer
         */
        MethodInfo getMethod();

        /**
         *
         * @return the observed type
         */
        Type getObservedType();

        /**
         *
         * @return the set of qualifiers
         */
        Set<AnnotationInstance> getQualifiers();

        /**
         *
         * @return the reception
         */
        Reception getReception();

        /**
         *
         * @return the transaction phase
         */
        TransactionPhase getTransactionPhase();

        /**
         *
         * @return true if the observer is asynchronous
         */
        boolean isAsync();

        /**
         * Retrieves all annotations declared on the observer method. This method is preferred to manual inspection
         * of {@link #getMethod()} which may, in some corner cases, hold outdated information.
         * <p>
         * The resulting set of annotations contains contains annotations that belong to the method itself
         * as well as to its parameters.
         *
         * @return collection of all annotations or an empty list in case of synthetic observer
         */
        Collection<AnnotationInstance> getAllAnnotations();

        /**
         * Instruct the container to ignore the observer method.
         *
         * @return self
         */
        void veto();

        /**
         * The transformation is not applied until the {@link AnnotationsTransformation#done()} method is invoked.
         *
         * @return a new transformation
         */
        ObserverTransformation transform();

    }

    interface ObserverTransformation extends AnnotationsTransformation<ObserverTransformation> {

        /**
         *
         * @param priority
         * @return self
         */
        ObserverTransformation priority(int priority);

        /**
         *
         * @param reception
         * @return self
         */
        ObserverTransformation reception(Reception reception);

        /**
         *
         * @param reception
         * @return self
         */
        ObserverTransformation transactionPhase(TransactionPhase transactionPhase);

        /**
         *
         * @param value
         * @return
         */
        ObserverTransformation async(boolean value);
    }

}
