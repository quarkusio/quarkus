package io.quarkus.arc.impl;

import static java.util.function.Predicate.not;

import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.RemovedBean;
import io.quarkus.arc.ResourceReferenceProvider;
import io.quarkus.arc.impl.ArcCDIProvider.ArcCDI;
import io.quarkus.arc.impl.EventImpl.Notifier;

public class ArcContainerImpl implements ArcContainer {

    private static final Logger LOGGER = Logger.getLogger(ArcContainerImpl.class.getPackage().getName());
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    private final String id;

    private final AtomicBoolean running;

    private final List<InjectableBean<?>> beans;
    private final LazyValue<List<RemovedBean>> removedBeans;
    private final List<InjectableInterceptor<?>> interceptors;
    private final List<InjectableDecorator<?>> decorators;
    private final List<InjectableObserverMethod<?>> observers;
    private final Contexts contexts;
    private final ComputingCache<Resolvable, Set<InjectableBean<?>>> resolved;
    private final ComputingCache<String, InjectableBean<?>> beansById;
    private final ComputingCache<String, Set<InjectableBean<?>>> beansByName;

    private final ArrayList<ResourceReferenceProvider> resourceProviders;

    final InstanceImpl<Object> instance;
    final Qualifiers registeredQualifiers;
    final InterceptorBindings registeredInterceptorBindings;

    private volatile ExecutorService executorService;

    private final CurrentContextFactory currentContextFactory;

    private final boolean strictMode;

    public ArcContainerImpl(CurrentContextFactory currentContextFactory, boolean strictMode) {
        this.strictMode = strictMode;
        id = String.valueOf(ID_GENERATOR.incrementAndGet());
        running = new AtomicBoolean(true);
        List<InjectableBean<?>> beans = new ArrayList<>();
        List<Supplier<Collection<RemovedBean>>> removedBeans = new ArrayList<>();
        List<InjectableInterceptor<?>> interceptors = new ArrayList<>();
        List<InjectableDecorator<?>> decorators = new ArrayList<>();
        List<InjectableObserverMethod<?>> observers = new ArrayList<>();
        Set<String> interceptorBindings = new HashSet<>();
        Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings = new HashMap<>();
        Map<String, Set<String>> qualifierNonbindingMembers = new HashMap<>();
        Set<String> qualifiers = new HashSet<>();
        this.currentContextFactory = currentContextFactory == null ? new ThreadLocalCurrentContextFactory()
                : currentContextFactory;

        List<Components> components = new ArrayList<>();
        for (ComponentsProvider componentsProvider : ServiceLoader.load(ComponentsProvider.class)) {
            components.add(componentsProvider.getComponents());
        }

        for (Components c : components) {
            for (InjectableBean<?> bean : c.getBeans()) {
                if (bean instanceof InjectableInterceptor) {
                    interceptors.add((InjectableInterceptor<?>) bean);
                } else if (bean instanceof InjectableDecorator) {
                    decorators.add((InjectableDecorator<?>) bean);
                } else {
                    beans.add(bean);
                }
            }
            removedBeans.add(c.getRemovedBeans());
            observers.addAll(c.getObservers());
            interceptorBindings.addAll(c.getInterceptorBindings());
            transitiveInterceptorBindings.putAll(c.getTransitiveInterceptorBindings());
            qualifierNonbindingMembers.putAll(c.getQualifierNonbindingMembers());
            qualifiers.addAll(c.getQualifiers());
        }

        // register built-in beans
        addBuiltInBeans(beans);

        interceptors.sort(Comparator.comparingInt(InjectableInterceptor::getPriority));
        decorators.sort(Comparator.comparingInt(InjectableDecorator::getPriority));

        resolved = new ComputingCache<>(this::resolve);
        beansById = new ComputingCache<>(this::findById);
        beansByName = new ComputingCache<>(this::resolve);
        resourceProviders = new ArrayList<>();
        for (ResourceReferenceProvider resourceProvider : ServiceLoader.load(ResourceReferenceProvider.class)) {
            resourceProviders.add(resourceProvider);
        }
        resourceProviders.trimToSize();

        instance = InstanceImpl.forGlobalEntrypoint(Object.class, Collections.emptySet());

        this.beans = List.copyOf(beans);
        this.interceptors = List.copyOf(interceptors);
        this.decorators = List.copyOf(decorators);
        this.observers = List.copyOf(observers);
        this.removedBeans = new LazyValue<>(new Supplier<List<RemovedBean>>() {
            @Override
            public List<RemovedBean> get() {
                List<RemovedBean> removed = new ArrayList<>();
                for (Supplier<Collection<RemovedBean>> supplier : removedBeans) {
                    removed.addAll(supplier.get());
                }
                LOGGER.debugf("Loaded %s removed beans lazily", removed.size());
                return List.copyOf(removed);
            }
        });
        this.registeredQualifiers = new Qualifiers(qualifiers, qualifierNonbindingMembers);
        this.registeredInterceptorBindings = new InterceptorBindings(interceptorBindings, transitiveInterceptorBindings);

        Contexts.Builder contextsBuilder = new Contexts.Builder(
                new RequestContext(this.currentContextFactory.create(RequestScoped.class),
                        notifierOrNull(Set.of(Initialized.Literal.REQUEST, Any.Literal.INSTANCE)),
                        notifierOrNull(Set.of(BeforeDestroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                        notifierOrNull(Set.of(Destroyed.Literal.REQUEST, Any.Literal.INSTANCE))),
                new ApplicationContext(),
                new SingletonContext(),
                new DependentContext());

        // Add custom contexts
        for (Components c : components) {
            for (InjectableContext context : c.getContexts()) {
                if (ApplicationScoped.class.equals(context.getScope())) {
                    throw new IllegalStateException(
                            "Failed to register a context - built-in application context is always active: " + context);
                }
                if (Singleton.class.equals(context.getScope())) {
                    throw new IllegalStateException(
                            "Failed to register a context - built-in singleton context is always active: " + context);
                }
                contextsBuilder.putContext(context);
            }
        }

        this.contexts = contextsBuilder.build();
    }

    public void init() {
        // Fire an event with qualifier @Initialized(ApplicationScoped.class)
        Set<Annotation> qualifiers = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        EventImpl.createNotifier(Object.class, Object.class, qualifiers, this, false, null)
                .notify("@Initialized(ApplicationScoped.class)");
        // Configure CDIProvider used for CDI.current()
        CDI.setCDIProvider(new ArcCDIProvider());
        LOGGER.debugf("ArC DI container initialized [beans=%s, observers=%s]", beans.size(), observers.size());
    }

    @Override
    public InjectableContext getActiveContext(Class<? extends Annotation> scopeType) {
        return contexts.getActiveContext(scopeType);
    }

    @Override
    public List<InjectableContext> getContexts(Class<? extends Annotation> scopeType) {
        return contexts.getContexts(scopeType);
    }

    @Override
    public Set<Class<? extends Annotation>> getScopes() {
        return contexts.scopes;
    }

    @Override
    public <T> InstanceHandle<T> instance(Class<T> type, Annotation... qualifiers) {
        return instanceHandle(type, qualifiers);
    }

    @Override
    public <T> InstanceHandle<T> instance(TypeLiteral<T> type, Annotation... qualifiers) {
        return instanceHandle(type.getType(), qualifiers);
    }

    @Override
    public <X> InstanceHandle<X> instance(Type type, Annotation... qualifiers) {
        return instanceHandle(type, qualifiers);
    }

    @Override
    public <T> Supplier<InstanceHandle<T>> beanInstanceSupplier(Class<T> type, Annotation... qualifiers) {
        return createInstanceSupplier(false, type, qualifiers);
    }

    @Override
    public <T> Supplier<InstanceHandle<T>> instanceSupplier(Class<T> type, Annotation... qualifiers) {
        return createInstanceSupplier(true, type, qualifiers);
    }

    private <T> Supplier<InstanceHandle<T>> createInstanceSupplier(boolean resolveAmbiguities, Class<T> type,
            Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        }
        Set<InjectableBean<?>> resolvedBeans = resolved.getValue(new Resolvable(type, qualifiers));
        Set<InjectableBean<?>> filteredBean = resolvedBeans;
        if (resolvedBeans.size() > 1) {
            if (resolveAmbiguities) {
                // this is non-standard CDI behavior that we momentarily keep to retain compatibility
                // if there are multiple beans we look for an exact match
                // this method is only called with the exact type required
                // so ignoring subclasses is the correct behaviour
                filteredBean = new HashSet<>();
                for (InjectableBean<?> i : resolvedBeans) {
                    if (i.getBeanClass().equals(type)) {
                        filteredBean.add(i);
                    }
                }
            } else {
                throw new AmbiguousResolutionException("Beans: " + resolvedBeans);
            }
        }
        @SuppressWarnings("unchecked")
        InjectableBean<T> bean = filteredBean.size() != 1 ? null
                : (InjectableBean<T>) filteredBean.iterator().next();
        if (bean == null) {
            return null;
        }
        return new Supplier<InstanceHandle<T>>() {
            @Override
            public InstanceHandle<T> get() {
                return beanInstanceHandle(bean, null);
            }
        };
    }

    @Override
    public <T> InstanceHandle<T> instance(InjectableBean<T> bean) {
        Objects.requireNonNull(bean);
        return beanInstanceHandle(bean, null);
    }

    @Override
    public <T> InjectableInstance<T> select(Class<T> type, Annotation... qualifiers) {
        return instance.select(type, qualifiers);
    }

    @Override
    public <T> InjectableInstance<T> select(TypeLiteral<T> type, Annotation... qualifiers) {
        return instance.select(type, qualifiers);
    }

    @Override
    public <T> List<InstanceHandle<T>> listAll(Class<T> type, Annotation... qualifiers) {
        return Instances.listOfHandles(CurrentInjectionPointProvider.EMPTY_SUPPLIER, type, Set.of(qualifiers),
                new CreationalContextImpl<>(null));
    }

    @Override
    public <T> List<InstanceHandle<T>> listAll(TypeLiteral<T> type, Annotation... qualifiers) {
        return Instances.listOfHandles(CurrentInjectionPointProvider.EMPTY_SUPPLIER, type.getType(), Set.of(qualifiers),
                new CreationalContextImpl<>(null));
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> InjectableBean<T> bean(String beanIdentifier) {
        Objects.requireNonNull(beanIdentifier);
        return (InjectableBean<T>) beansById.getValue(beanIdentifier);
    }

    @Override
    public InjectableBean<?> namedBean(String name) {
        Objects.requireNonNull(name);
        Set<InjectableBean<?>> found = beansByName.getValue(name);
        return found.size() == 1 ? found.iterator().next() : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> InstanceHandle<T> instance(String name) {
        Objects.requireNonNull(name);
        Set<InjectableBean<?>> resolvedBeans = beansByName.getValue(name);
        return resolvedBeans.size() != 1 ? EagerInstanceHandle.unavailable()
                : (InstanceHandle<T>) beanInstanceHandle(resolvedBeans.iterator()
                        .next(), null);
    }

    @Override
    public ManagedContext requestContext() {
        return contexts.requestContext;
    }

    @Override
    public BeanManager beanManager() {
        return BeanManagerImpl.INSTANCE.get();
    }

    @Override
    public ExecutorService getExecutorService() {
        ExecutorService executor = executorService;
        return executor != null ? executor : ForkJoinPool.commonPool();
    }

    public void setExecutor(ExecutorService executor) {
        this.executorService = executor;
    }

    @Override
    public CurrentContextFactory getCurrentContextFactory() {
        return currentContextFactory;
    }

    @Override
    public boolean strictCompatibility() {
        return strictMode;
    }

    @Override
    public String toString() {
        return "ArcContainerImpl [id=" + id + ", running=" + running + ", beans=" + beans.size() + ", observers="
                + observers.size() + ", scopes="
                + contexts.scopes.size() + "]";
    }

    public synchronized void shutdown() {
        if (running.get()) {
            // Make sure all dependent bean instances obtained via CDI.current() are destroyed correctly
            CDI<?> cdi = CDI.current();
            if (cdi instanceof ArcCDI) {
                ArcCDI arcCdi = (ArcCDI) cdi;
                arcCdi.destroy();
            }
            // Terminate request context if for any reason is still active
            contexts.requestContext.terminate();
            // Fire an event with qualifier @BeforeDestroyed(ApplicationScoped.class)
            Set<Annotation> beforeDestroyQualifiers = new HashSet<>(4);
            beforeDestroyQualifiers.add(BeforeDestroyed.Literal.APPLICATION);
            beforeDestroyQualifiers.add(Any.Literal.INSTANCE);
            try {
                EventImpl.createNotifier(Object.class, Object.class, beforeDestroyQualifiers, this, false, null)
                        .notify(toString());
            } catch (Exception e) {
                LOGGER.warn("An error occurred during delivery of the @BeforeDestroyed(ApplicationScoped.class) event", e);
            }
            // Destroy contexts
            contexts.applicationContext.destroy();
            // Fire an event with qualifier @Destroyed(ApplicationScoped.class)
            Set<Annotation> destroyQualifiers = new HashSet<>(4);
            destroyQualifiers.add(Destroyed.Literal.APPLICATION);
            destroyQualifiers.add(Any.Literal.INSTANCE);
            try {
                EventImpl.createNotifier(Object.class, Object.class, destroyQualifiers, this, false, null).notify(toString());
            } catch (Exception e) {
                LOGGER.warn("An error occurred during delivery of the @Destroyed(ApplicationScoped.class) event", e);
            }
            contexts.singletonContext.destroy();

            // Clear caches
            Reflections.clearCaches();
            resolved.clear();
            running.set(false);
            InterceptedStaticMethods.clear();

            LOGGER.debugf("ArC DI container shut down");
        }
    }

    public List<InjectableBean<?>> getBeans() {
        return beans;
    }

    public List<RemovedBean> getRemovedBeans() {
        return removedBeans.get();
    }

    public List<InjectableInterceptor<?>> getInterceptors() {
        return interceptors;
    }

    public List<InjectableDecorator<?>> getDecorators() {
        return decorators;
    }

    public List<InjectableObserverMethod<?>> getObservers() {
        return observers;
    }

    InstanceHandle<Object> getResource(Type type, Set<Annotation> annotations) {
        for (ResourceReferenceProvider resourceProvider : resourceProviders) {
            InstanceHandle<Object> ret = resourceProvider.get(type, annotations);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private Notifier<Object> notifierOrNull(Set<Annotation> qualifiers) {
        Notifier<Object> notifier = EventImpl.createNotifier(Object.class, Object.class,
                qualifiers, this, false, null);
        return notifier.isEmpty() ? null : notifier;
    }

    private static void addBuiltInBeans(List<InjectableBean<?>> beans) {
        // BeanManager, Event<?>, Instance<?>, InjectionPoint
        beans.add(new BeanManagerBean());
        beans.add(new EventBean());
        beans.add(InstanceBean.INSTANCE);
        beans.add(new InjectionPointBean());
    }

    private <T> InstanceHandle<T> instanceHandle(Type type, Annotation... qualifiers) {
        return beanInstanceHandle(getBean(type, qualifiers), null);
    }

    static <T> InstanceHandle<T> beanInstanceHandle(InjectableBean<T> bean, CreationalContextImpl<T> parentContext,
            boolean resetCurrentInjectionPoint, Consumer<T> destroyLogic) {
        return beanInstanceHandle(bean, parentContext, resetCurrentInjectionPoint, destroyLogic, false);
    }

    static <T> InstanceHandle<T> beanInstanceHandle(InjectableBean<T> bean, CreationalContextImpl<T> parentContext,
            boolean resetCurrentInjectionPoint, Consumer<T> destroyLogic, boolean useParentCreationalContextDirectly) {
        if (bean != null) {
            if (parentContext == null && Dependent.class.equals(bean.getScope())) {
                parentContext = new CreationalContextImpl<>(null);
            }
            CreationalContextImpl<T> creationalContext;
            if (parentContext != null) {
                creationalContext = useParentCreationalContextDirectly ? parentContext : parentContext.child(bean);
            } else {
                creationalContext = new CreationalContextImpl<>(bean);
            }
            InjectionPoint prev = null;
            if (resetCurrentInjectionPoint) {
                prev = InjectionPointProvider.set(CurrentInjectionPointProvider.EMPTY);
            }
            try {
                return new EagerInstanceHandle<>(bean, bean.get(creationalContext), creationalContext, parentContext,
                        destroyLogic);
            } finally {
                if (resetCurrentInjectionPoint) {
                    InjectionPointProvider.set(prev);
                }
            }
        } else {
            return EagerInstanceHandle.unavailable();
        }
    }

    static <T> InstanceHandle<T> beanInstanceHandle(InjectableBean<T> bean, CreationalContextImpl<T> parentContext) {
        return beanInstanceHandle(bean, parentContext, true, null);
    }

    @SuppressWarnings("unchecked")
    private <T> InjectableBean<T> getBean(Type requiredType, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            registeredQualifiers.verify(qualifiers);
        }
        Resolvable resolvable = new Resolvable(requiredType, qualifiers);
        Set<InjectableBean<?>> resolvedBeans = resolved.getValue(resolvable);
        if (resolvedBeans.isEmpty()) {
            scanRemovedBeans(resolvable);
        }
        return resolvedBeans.size() != 1 ? null : (InjectableBean<T>) resolvedBeans.iterator().next();
    }

    Set<Bean<?>> getBeans(Type requiredType, Annotation... qualifiers) {
        if (requiredType instanceof TypeVariable) {
            throw new IllegalArgumentException("The given type is a type variable: " + requiredType);
        }
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            registeredQualifiers.verify(qualifiers);
        }
        // This method does not cache the results
        return Set.of(getMatchingBeans(new Resolvable(requiredType, qualifiers)).toArray(new Bean<?>[] {}));
    }

    Set<Bean<?>> getBeans(String name) {
        // This method does not cache the results
        return new HashSet<>(getMatchingBeans(name));
    }

    boolean isScope(Class<? extends Annotation> annotationType) {
        if (annotationType.isAnnotationPresent(Scope.class) || annotationType.isAnnotationPresent(NormalScope.class)) {
            return true;
        }
        for (Class<? extends Annotation> scopeType : contexts.scopes) {
            if (scopeType.equals(annotationType)) {
                return true;
            }
        }
        return false;
    }

    boolean isNormalScope(Class<? extends Annotation> annotationType) {
        if (annotationType.isAnnotationPresent(NormalScope.class)) {
            return true;
        }
        List<InjectableContext> injectableContexts = contexts.getContexts(annotationType);
        for (InjectableContext context : injectableContexts) {
            if (context.isNormal()) {
                return true;
            }
        }
        return false;
    }

    private Set<InjectableBean<?>> resolve(Resolvable resolvable) {
        return resolve(getMatchingBeans(resolvable));
    }

    private Set<InjectableBean<?>> resolve(String name) {
        return resolve(getMatchingBeans(name));
    }

    private InjectableBean<?> findById(String identifier) {
        for (InjectableBean<?> bean : beans) {
            if (bean.getIdentifier().equals(identifier)) {
                return bean;
            }
        }
        for (InjectableInterceptor<?> interceptorBean : interceptors) {
            if (interceptorBean.getIdentifier().equals(identifier)) {
                return interceptorBean;
            }
        }
        for (InjectableDecorator<?> decoratorBean : decorators) {
            if (decoratorBean.getIdentifier().equals(identifier)) {
                return decoratorBean;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
        if (beans == null || beans.isEmpty()) {
            return null;
        } else if (beans.size() == 1) {
            return beans.iterator().next();
        } else {
            // Try to resolve the ambiguity
            if (beans.stream().allMatch(InjectableBean.class::isInstance)) {
                List<InjectableBean<?>> matching = new ArrayList<>(beans.size());
                for (Bean<? extends X> bean : beans) {
                    matching.add((InjectableBean<? extends X>) bean);
                }
                Set<InjectableBean<?>> resolved = resolve(matching);
                if (resolved.size() != 1) {
                    throw new AmbiguousResolutionException(resolved.toString());
                }
                return (Bean<? extends X>) resolved.iterator().next();
            } else {
                // The set contains non-Arc beans - give our best effort
                Set<Bean<? extends X>> resolved = new HashSet<>(beans);
                resolved.removeIf(not(Bean::isAlternative));
                if (resolved.size() != 1) {
                    throw new AmbiguousResolutionException(resolved.toString());
                }
                return resolved.iterator().next();
            }
        }
    }

    private static Set<InjectableBean<?>> resolve(List<InjectableBean<?>> matching) {
        if (matching.isEmpty()) {
            return Collections.emptySet();
        } else if (matching.size() == 1) {
            return Set.of(matching.get(0));
        }
        // Try to resolve the ambiguity and return the set of disambiguated beans

        // First remove the default beans
        List<InjectableBean<?>> nonDefault = new ArrayList<>(matching);
        nonDefault.removeIf(InjectableBean::isDefaultBean);
        if (nonDefault.isEmpty()) {
            // All the matching beans were default
            return Set.copyOf(matching);
        } else if (nonDefault.size() == 1) {
            return Set.of(nonDefault.get(0));
        }

        // More than one non-default bean remains - eliminate beans that don't have a priority
        List<InjectableBean<?>> priorityBeans = new ArrayList<>(nonDefault);
        priorityBeans.removeIf(not(ArcContainerImpl::isAlternativeOrDeclaredOnAlternative));
        if (priorityBeans.isEmpty()) {
            // No alternative/priority beans are present
            return Set.copyOf(nonDefault);
        } else if (priorityBeans.size() == 1) {
            return Set.of(priorityBeans.get(0));
        } else {
            // Keep only the highest priorities
            priorityBeans.sort(ArcContainerImpl::compareAlternativeBeans);
            Integer highest = getAlternativePriority(priorityBeans.get(0));
            priorityBeans.removeIf(bean -> !highest.equals(getAlternativePriority(bean)));
            if (priorityBeans.size() == 1) {
                return Set.of(priorityBeans.get(0));
            }
            return Set.copyOf(priorityBeans);
        }
    }

    private static boolean isAlternativeOrDeclaredOnAlternative(InjectableBean<?> bean) {
        return bean.getAlternativePriority() != null
                || bean.getDeclaringBean() != null && bean.getDeclaringBean().getAlternativePriority() != null;
    }

    private static Integer getAlternativePriority(InjectableBean<?> bean) {
        Integer beanPriority = bean.getAlternativePriority();
        if (beanPriority == null && bean.getDeclaringBean() != null) {
            beanPriority = bean.getDeclaringBean().getAlternativePriority();
        }
        return beanPriority;
    }

    List<InjectableBean<?>> getMatchingBeans(Resolvable resolvable) {
        List<InjectableBean<?>> matching = new ArrayList<>();
        for (InjectableBean<?> bean : beans) {
            if (matches(bean, resolvable.requiredType, resolvable.qualifiers)) {
                matching.add(bean);
            }
        }
        return matching;
    }

    List<RemovedBean> getMatchingRemovedBeans(Resolvable resolvable) {
        List<RemovedBean> matching = new ArrayList<>();
        for (RemovedBean removedBean : removedBeans.get()) {
            if (matches(removedBean.getTypes(), removedBean.getQualifiers(), resolvable.requiredType,
                    resolvable.qualifiers)) {
                matching.add(removedBean);
            }
        }
        return matching;
    }

    void scanRemovedBeans(Type requiredType, Annotation... qualifiers) {
        scanRemovedBeans(new Resolvable(requiredType, qualifiers));
    }

    void scanRemovedBeans(Resolvable resolvable) {
        List<RemovedBean> removedMatching = getMatchingRemovedBeans(resolvable);
        if (!removedMatching.isEmpty()) {
            String separator = "====================";
            String msg = "\n%1$s%1$s%1$s%1$s\n"
                    + "CDI: programmatic lookup problem detected\n"
                    + "-----------------------------------------\n"
                    + "At least one bean matched the required type and qualifiers but was marked as unused and removed during build\n\n"
                    + "Stack frame: %5$s\n"
                    + "Required type: %3$s\n"
                    + "Required qualifiers: %4$s\n"
                    + "Removed beans:\n\t- %2$s\n"
                    + "Solutions:\n"
                    + "\t- Application developers can eliminate false positives via the @Unremovable annotation\n"
                    + "\t- Extensions can eliminate false positives via build items, e.g. using the UnremovableBeanBuildItem\n"
                    + "\t- See also https://quarkus.io/guides/cdi-reference#remove_unused_beans\n"
                    + "\t- Enable the DEBUG log level to see the full stack trace\n"
                    + "%1$s%1$s%1$s%1$s\n";
            StackWalker walker = StackWalker.getInstance();
            StackFrame frame = walker.walk(this::findCaller);
            LOGGER.warnf(msg, separator,
                    removedMatching.stream().map(Object::toString).collect(Collectors.joining("\n\t- ")),
                    resolvable.requiredType, Arrays.toString(resolvable.qualifiers), frame != null ? frame : "n/a");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\nCDI: programmatic lookup stack trace:\n" + walker.walk(this::collectStack));
            }
        }
    }

    private StackFrame findCaller(Stream<StackFrame> stream) {
        return stream
                .filter(this::isCallerFrame)
                .findFirst().orElse(null);
    }

    private String collectStack(Stream<StackFrame> stream) {
        return stream
                .map(Object::toString)
                .collect(Collectors.joining("\n\t"));
    }

    private boolean isCallerFrame(StackFrame frame) {
        String className = frame.getClassName();
        return !className.startsWith("io.quarkus.arc.impl");
    }

    List<InjectableBean<?>> getMatchingBeans(String name) {
        List<InjectableBean<?>> matching = new ArrayList<>();
        for (InjectableBean<?> bean : beans) {
            if (name.equals(bean.getName())) {
                matching.add(bean);
            }
        }
        return matching;
    }

    private static int compareAlternativeBeans(InjectableBean<?> bean1, InjectableBean<?> bean2) {
        // The highest priority wins
        Integer priority2 = bean2.getAlternativePriority();
        if (priority2 == null && bean2.getDeclaringBean() != null) {
            priority2 = bean2.getDeclaringBean().getAlternativePriority();
        }
        Integer priority1 = bean1.getAlternativePriority();
        if (priority1 == null && bean1.getDeclaringBean() != null) {
            priority1 = bean1.getDeclaringBean().getAlternativePriority();
        }
        return priority2.compareTo(priority1);
    }

    @SuppressWarnings("unchecked")
    <T> List<InjectableObserverMethod<? super T>> resolveObservers(Type eventType, Set<Annotation> eventQualifiers) {
        registeredQualifiers.verify(eventQualifiers);
        if (observers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Type> eventTypes = new HierarchyDiscovery(eventType).getTypeClosure();
        List<InjectableObserverMethod<? super T>> resolvedObservers = new ArrayList<>();
        for (InjectableObserverMethod<?> observer : observers) {
            if (EventTypeAssignabilityRules.instance().matches(observer.getObservedType(), eventTypes)) {
                if (observer.getObservedQualifiers().isEmpty()
                        || registeredQualifiers.isSubset(observer.getObservedQualifiers(), eventQualifiers)) {
                    resolvedObservers.add((InjectableObserverMethod<? super T>) observer);
                }
            }
        }
        // Observers with smaller priority values are called first
        resolvedObservers.sort(InjectableObserverMethod::compare);
        return resolvedObservers;
    }

    List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
        if (interceptors.isEmpty()) {
            return Collections.emptyList();
        }
        if (interceptorBindings.length == 0) {
            throw new IllegalArgumentException("No interceptor bindings");
        }
        registeredInterceptorBindings.verify(interceptorBindings);
        List<Interceptor<?>> interceptors = new ArrayList<>();
        List<Annotation> bindings = new ArrayList<>();
        for (Annotation binding : interceptorBindings) {
            bindings.add(binding);
            Set<Annotation> transitive = registeredInterceptorBindings.getTransitive(binding.annotationType());
            if (transitive != null) {
                bindings.addAll(transitive);
            }
        }
        for (InjectableInterceptor<?> interceptor : this.interceptors) {
            if (interceptor.intercepts(type) && hasAllInterceptionBindings(interceptor, bindings)) {
                interceptors.add(interceptor);
            }
        }
        return interceptors;
    }

    List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
        if (decorators.isEmpty()) {
            return Collections.emptyList();
        }
        if (Objects.requireNonNull(types).isEmpty()) {
            throw new IllegalArgumentException("The set of bean types must not be empty");
        }
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            registeredQualifiers.verify(qualifiers);
        }
        List<Decorator<?>> decorators = new ArrayList<>();
        for (InjectableDecorator<?> decorator : this.decorators) {
            if (decoratorMatches(decorator.getDelegateType(), decorator.getDelegateQualifiers(), types, Set.of(qualifiers))) {
                decorators.add(decorator);
            }
        }
        return decorators;
    }

    private boolean hasAllInterceptionBindings(InjectableInterceptor<?> interceptor, Iterable<Annotation> bindings) {
        // The method or constructor has all the interceptor bindings of the interceptor
        for (Annotation binding : interceptor.getInterceptorBindings()) {
            // The resolution rules are the same for qualifiers
            if (!registeredQualifiers.hasQualifier(bindings, binding)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs typesafe resolution and resolves ambiguities.
     *
     * @param requiredType
     * @param qualifiers
     * @return the set of resolved beans
     */
    Set<InjectableBean<?>> getResolvedBeans(Type requiredType, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            registeredQualifiers.verify(qualifiers);
        }
        return resolved.getValue(new Resolvable(requiredType, qualifiers));
    }

    private boolean matches(InjectableBean<?> bean, Type requiredType, Annotation... qualifiers) {
        return matches(bean.getTypes(), bean.getQualifiers(), requiredType, qualifiers);
    }

    private boolean matches(Set<Type> beanTypes, Set<Annotation> beanQualifiers, Type requiredType, Annotation... qualifiers) {
        if (!BeanTypeAssignabilityRules.instance().matches(requiredType, beanTypes)) {
            return false;
        }
        return registeredQualifiers.hasQualifiers(beanQualifiers, qualifiers);
    }

    private boolean decoratorMatches(Type delegateType, Set<Annotation> delegateQualifiers, Set<Type> requiredTypes,
            Set<Annotation> requiredQualifiers) {
        if (!DelegateInjectionPointAssignabilityRules.instance().matches(delegateType, requiredTypes)) {
            return false;
        }
        return registeredQualifiers.hasQualifiers(delegateQualifiers, requiredQualifiers.toArray(new Annotation[0]));
    }

    static ArcContainerImpl unwrap(ArcContainer container) {
        if (container instanceof ArcContainerImpl) {
            return (ArcContainerImpl) container;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void mockObservers(String beanIdentifier, boolean mock) {
        instance().mockObserversFor(beanIdentifier, mock);
    }

    private void mockObserversFor(String beanIdentifier, boolean mock) {
        for (InjectableObserverMethod<?> observer : observers) {
            if (observer instanceof Mockable && beanIdentifier.equals(observer.getDeclaringBeanIdentifier())) {
                Mockable mockable = (Mockable) observer;
                if (mock) {
                    mockable.arc$setMock(null);
                } else {
                    mockable.arc$clearMock();
                }
            }
        }
    }

    public void mockObserversFor(Class<?> beanClass, boolean mock) {
        for (InjectableObserverMethod<?> observer : observers) {
            if (observer instanceof Mockable && beanClass.equals(observer.getBeanClass())) {
                Mockable mockable = (Mockable) observer;
                if (mock) {
                    mockable.arc$setMock(null);
                } else {
                    mockable.arc$clearMock();
                }
            }
        }
    }

    public static ArcContainerImpl instance() {
        return unwrap(Arc.container());
    }

    private static final class Resolvable {

        private static final Annotation[] ANY_QUALIFIER = { Any.Literal.INSTANCE };

        final Type requiredType;

        final Annotation[] qualifiers;

        Resolvable(Type requiredType, Annotation[] qualifiers) {
            // if the type is Event, Instance or InjectionPoint (the built-in types), the resolution simplifies
            // type to raw type and ignores qualifiers
            // this is so that every injection point matches the bean we provide for that type
            Type rawType = Reflections.getRawType(requiredType);
            if (Event.class.equals(rawType) || Instance.class.equals(rawType) || InjectionPoint.class.equals(rawType)) {
                this.requiredType = rawType;
                this.qualifiers = ANY_QUALIFIER;
            } else {
                this.requiredType = requiredType;
                this.qualifiers = qualifiers;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(qualifiers);
            result = prime * result + (requiredType == null ? 0 : requiredType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Resolvable)) {
                return false;
            }
            Resolvable other = (Resolvable) obj;
            if (requiredType == null) {
                if (other.requiredType != null) {
                    return false;
                }
            } else if (!requiredType.equals(other.requiredType)) {
                return false;
            }
            return Arrays.equals(qualifiers, other.qualifiers);
        }

    }
}
