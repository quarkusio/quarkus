package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

/**
 * Convenient {@link Stream} wrapper that can be used to filter a set of beans.
 * <p>
 * This object is stateful and cannot be reused. After a terminal opration is performed, the underlying stream is considered
 * consumed, and can no longer be used.
 * <p>
 * This construct is not threadsafe.
 */
public final class BeanStream implements Iterable<BeanInfo> {

    private Stream<BeanInfo> stream;

    public BeanStream(Collection<BeanInfo> beans) {
        this.stream = Objects.requireNonNull(beans, "Beans collection is null").stream();
    }

    /**
     * 
     * @param scopeName
     * @return the new stream of beans
     * @see BeanInfo#getScope()
     */
    public BeanStream withScope(Class<? extends Annotation> scope) {
        return withScope(DotName.createSimple(scope.getName()));
    }

    /**
     * 
     * @param scopeName
     * @return the new stream of beans
     * @see BeanInfo#getScope()
     */
    public BeanStream withScope(DotName scopeName) {
        stream = stream.filter(bean -> bean.getScope().getDotName().equals(scopeName));
        return this;
    }

    /**
     * 
     * @param beanType
     * @return the new stream of beans
     * @see BeanInfo#getTypes()
     */
    public BeanStream withBeanType(Class<?> beanType) {
        return withBeanType(DotName.createSimple(beanType.getName()));
    }

    /**
     * 
     * @param beanType
     * @return the new stream of beans
     * @see BeanInfo#getTypes()
     */
    public BeanStream withBeanType(DotName beanTypeName) {
        stream = stream.filter(bean -> bean.getTypes().stream().anyMatch(t -> t.name().equals(beanTypeName)));
        return this;
    }

    /**
     * 
     * @param beanType
     * @return the new stream of beans
     * @see BeanInfo#getTypes()
     */
    public BeanStream withBeanType(Type beanType) {
        stream = stream.filter(bean -> bean.getTypes().stream().anyMatch(t -> t.equals(beanType)));
        return this;
    }

    /**
     * 
     * @param beanClass
     * @return the new stream of beans
     * @see BeanInfo#getBeanClass()
     */
    public BeanStream withBeanClass(Class<?> beanClass) {
        return withBeanClass(DotName.createSimple(beanClass.getName()));
    }

    /**
     * 
     * @param predicate
     * @return the new stream of beans
     */
    public BeanStream matchBeanTypes(Predicate<Set<Type>> predicate) {
        stream = stream.filter(bean -> predicate.test(bean.getTypes()));
        return this;
    }

    /**
     * 
     * @return the new stream of beans
     * @see BeanInfo#getTarget()
     */
    public BeanStream withTarget() {
        stream = stream.filter(bean -> bean.getTarget().isPresent());
        return this;
    }

    /**
     * 
     * @param beanClass
     * @return the new stream of beans
     * @see BeanInfo#getBeanClass()
     */
    public BeanStream withBeanClass(DotName beanClass) {
        stream = stream.filter(bean -> bean.getBeanClass().equals(beanClass));
        return this;
    }

    /**
     * 
     * @param qualifier
     * @return the new stream of beans
     * @see BeanInfo#getQualifiers()
     */
    @SafeVarargs
    public final BeanStream withQualifier(Class<? extends Annotation>... qualifiers) {
        if (qualifiers.length == 1) {
            return withQualifier(DotName.createSimple(qualifiers[0].getName()));
        } else {
            return withQualifier(Arrays.stream(qualifiers).map(q -> DotName.createSimple(q.getName())).toArray(DotName[]::new));
        }
    }

    /**
     * 
     * @param qualifierNames
     * @return the new stream of beans
     * @see BeanInfo#getQualifiers()
     */
    public BeanStream withQualifier(DotName... qualifierNames) {
        if (qualifierNames.length == 1) {
            stream = stream.filter(bean -> bean.getQualifiers().stream().anyMatch(q -> q.name().equals(qualifierNames[0])));
        } else {
            stream = stream.filter(bean -> bean.getQualifiers().stream().anyMatch(q -> {
                for (DotName qualifierName : qualifierNames) {
                    if (q.name().equals(qualifierName)) {
                        return true;
                    }
                }
                return false;
            }));
        }
        return this;
    }

    /**
     * 
     * @param name
     * @return the new stream of beans
     * @see BeanInfo#getName()
     */
    public BeanStream withName(String name) {
        stream = stream.filter(bean -> name.equals(bean.getName()));
        return this;
    }

    /**
     * 
     * @return the new stream of beans
     * @see BeanInfo#getName()
     */
    public BeanStream withName() {
        stream = stream.filter(bean -> bean.getName() != null);
        return this;
    }

    /**
     * 
     * @param id
     * @return an {@link Optional} with the matching bean, or an empty {@link Optional} if no such bean is found
     * @see BeanInfo#getIdentifier()
     */
    public Optional<BeanInfo> findByIdentifier(String id) {
        return stream.filter(bean -> id.equals(bean.getIdentifier())).findFirst();
    }

    /**
     * 
     * @return the new stream of producer beans
     */
    public BeanStream producers() {
        stream = stream.filter(bean -> bean.isProducerField() || bean.isProducerMethod());
        return this;
    }

    /**
     * 
     * @return the new stream of producer method beans
     */
    public BeanStream producerMethods() {
        stream = stream.filter(BeanInfo::isProducerMethod);
        return this;
    }

    /**
     * 
     * @return the new stream of producer field beans
     */
    public BeanStream producerFields() {
        stream = stream.filter(BeanInfo::isProducerField);
        return this;
    }

    /**
     * 
     * @return the new stream of class beans
     */
    public BeanStream classBeans() {
        stream = stream.filter(BeanInfo::isClassBean);
        return this;
    }

    /**
     * 
     * @return the new stream of synthetic beans
     */
    public BeanStream syntheticBeans() {
        stream = stream.filter(BeanInfo::isSynthetic);
        return this;
    }

    /**
     * 
     * @return the new stream of named beans
     * @see BeanInfo#getName()
     */
    public BeanStream namedBeans() {
        stream = stream.filter(bean -> bean.getName() != null);
        return this;
    }

    /**
     * 
     * @return the new stream of default beans
     * @see BeanInfo#isDefaultBean()
     */
    public BeanStream defaultBeans() {
        stream = stream.filter(BeanInfo::isDefaultBean);
        return this;
    }

    /**
     * 
     * @return the new stream of default beans
     * @see BeanInfo#isAlternative()
     */
    public BeanStream alternativeBeans() {
        stream = stream.filter(BeanInfo::isAlternative);
        return this;
    }

    /**
     * 
     * @param requiredType
     * @param requiredQualifiers
     * @return the new stream of beans assignable to the required type and qualifiers
     */
    public BeanStream assignableTo(Type requiredType, AnnotationInstance... requiredQualifiers) {
        stream = stream.filter(bean -> bean.isAssignableTo(requiredType, requiredQualifiers));
        return this;
    }

    /**
     * 
     * @param predicate
     * @return the new stream
     */
    public BeanStream filter(Predicate<BeanInfo> predicate) {
        stream = stream.filter(predicate);
        return this;
    }

    /**
     * Terminal operation.
     * 
     * @return the list of beans
     */
    public List<BeanInfo> collect() {
        return stream.collect(Collectors.toList());
    }

    /**
     * 
     * @return the underlying stream instance
     */
    public Stream<BeanInfo> stream() {
        return stream;
    }

    /**
     * Terminal operation.
     * 
     * @return true if the stream contains no elements
     */
    public boolean isEmpty() {
        return stream.count() == 0;
    }

    /**
     * Terminal operation.
     * 
     * @return the iterator
     */
    @Override
    public Iterator<BeanInfo> iterator() {
        return stream.iterator();
    }

    /**
     * Terminal operation.
     * 
     * @return an {@link Optional} with the first matching bean, or an empty {@link Optional} if no bean is matching
     */
    public Optional<BeanInfo> firstResult() {
        return stream.findFirst();
    }

    /**
     * Terminal operation.
     * 
     * @param <R>
     * @param <A>
     * @param collector
     * @return the collected result
     */
    public <R, A> R collect(Collector<BeanInfo, A, R> collector) {
        return stream.collect(collector);
    }

}
