package io.quarkus.arc.impl;

import static java.util.function.Predicate.not;

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
import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Scope;
import javax.inject.Singleton;
import org.jboss.logging.Logger;

public class ArcContainerImpl implements ArcContainer {

    private static final Logger LOGGER = Logger.getLogger(ArcContainerImpl.class.getPackage().getName());
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    private final String id;

    private final AtomicBoolean running;

    private final List<InjectableBean<?>> beans;
    private final List<RemovedBean> removedBeans;
    private final List<InjectableInterceptor<?>> interceptors;
    private final List<InjectableDecorator<?>> decorators;
    private final List<InjectableObserverMethod<?>> observers;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;
    private final Map<String, Set<String>> qualifierNonbindingMembers;

    private final Map<Class<? extends Annotation>, List<InjectableContext>> contexts;
    private final ManagedContext requestContext;
    private final InjectableContext applicationContext;
    private final InjectableContext singletonContext;

    private final ComputingCache<Resolvable, Set<InjectableBean<?>>> resolved;
    private final ComputingCache<String, InjectableBean<?>> beansById;
    private final ComputingCache<String, Set<InjectableBean<?>>> beansByName;

    private final List<ResourceReferenceProvider> resourceProviders;

    final InstanceImpl<Object> instance;

    private volatile ExecutorService executorService;

    private final CurrentContextFactory currentContextFactory;

    public ArcContainerImpl(CurrentContextFactory currentContextFactory) {
        id = String.valueOf(ID_GENERATOR.incrementAndGet());
        running = new AtomicBoolean(true);
        List<InjectableBean<?>> beans = new ArrayList<>();
        List<RemovedBean> removedBeans = new ArrayList<>();
        List<InjectableInterceptor<?>> interceptors = new ArrayList<>();
        List<InjectableDecorator<?>> decorators = new ArrayList<>();
        List<InjectableObserverMethod<?>> observers = new ArrayList<>();
        Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings = new HashMap<>();
        Map<String, Set<String>> qualifierNonbindingMembers = new HashMap<>();
        this.currentContextFactory = currentContextFactory == null ? new ThreadLocalCurrentContextFactory()
                : currentContextFactory;

        applicationContext = new ApplicationContext();
        singletonContext = new SingletonContext();
        requestContext = new RequestContext(this.currentContextFactory.create(RequestScoped.class));
        contexts = new HashMap<>();
        putContext(requestContext);
        putContext(applicationContext);
        putContext(singletonContext);

        for (ComponentsProvider componentsProvider : loadComponentsProviders()) {
            Components components = componentsProvider.getComponents();
            for (InjectableBean<?> bean : components.getBeans()) {
                if (bean instanceof InjectableInterceptor) {
                    interceptors.add((InjectableInterceptor<?>) bean);
                } else if (bean instanceof InjectableDecorator) {
                    decorators.add((InjectableDecorator<?>) bean);
                } else {
                    beans.add(bean);
                }
            }
            removedBeans.addAll(components.getRemovedBeans());
            observers.addAll(components.getObservers());
            // Add custom contexts
            for (InjectableContext context : components.getContexts()) {
                if (ApplicationScoped.class.equals(context.getScope())) {
                    throw new IllegalStateException(
                            "Failed to register a context - built-in application context is always active: " + context);
                }
                if (Singleton.class.equals(context.getScope())) {
                    throw new IllegalStateException(
                            "Failed to register a context - built-in singleton context is always active: " + context);
                }
                putContext(context);
            }
            transitiveInterceptorBindings.putAll(components.getTransitiveInterceptorBindings());
            qualifierNonbindingMembers.putAll(components.getQualifierNonbindingMembers());
        }
        // register built-in beans
        addBuiltInBeans(beans);

        interceptors.sort((i1, i2) -> Integer.compare(i2.getPriority(), i1.getPriority()));

        resolved = new ComputingCache<>(this::resolve);
        beansById = new ComputingCache<>(this::findById);
        beansByName = new ComputingCache<>(this::resolve);
        resourceProviders = loadResourceProviders();

        instance = InstanceImpl.of(Object.class, Collections.emptySet());

        this.beans = List.copyOf(beans);
        this.interceptors = List.copyOf(interceptors);
        this.decorators = List.copyOf(decorators);
        this.observers = List.copyOf(observers);
        this.removedBeans = List.copyOf(removedBeans);
        this.transitiveInterceptorBindings = Map.copyOf(transitiveInterceptorBindings);
        this.qualifierNonbindingMembers = Map.copyOf(qualifierNonbindingMembers);
    }

    private Iterable<ComponentsProvider> loadComponentsProviders() {
        return ServiceLoader.load(ComponentsProvider.class);
    }

    private List<ResourceReferenceProvider> loadResourceProviders() {
        final ArrayList<ResourceReferenceProvider> resourceProviders;
        resourceProviders = new ArrayList<>();
        for (ResourceReferenceProvider resourceProvider : ServiceLoader.load(ResourceReferenceProvider.class)) {
            resourceProviders.add(resourceProvider);
        }
        resourceProviders.trimToSize();
        return resourceProviders;
    }

    private void putContext(InjectableContext context) {
        Collection<InjectableContext> values = contexts.get(context.getScope());
        if (values == null) {
            contexts.put(context.getScope(), Collections.singletonList(context));
        } else {
            List<InjectableContext> multi = new ArrayList<>(values.size() + 1);
            multi.addAll(values);
            multi.add(context);
            contexts.put(context.getScope(), Collections.unmodifiableList(multi));
        }
    }

    private static void addBuiltInBeans(List<InjectableBean<?>> beans) {
        // BeanManager, Event<?>, Instance<?>
        beans.add(new BeanManagerBean());
        beans.add(new EventBean());
        beans.add(InstanceBean.INSTANCE);
    }

    public void init() {
        // Fire an event with qualifier @Initialized(ApplicationScoped.class)
        Set<Annotation> qualifiers = Set.of(Initialized.Literal.APPLICATION, Any.Literal.INSTANCE);
        EventImpl.createNotifier(Object.class, Object.class, qualifiers, this, false)
                .notify("@Initialized(ApplicationScoped.class)");
        // Configure CDIProvider used for CDI.current()
        CDI.setCDIProvider(new ArcCDIProvider());
        LOGGER.debugf("ArC DI container initialized [beans=%s, observers=%s]", beans.size(), observers.size());
    }

    @Override
    public InjectableContext getActiveContext(Class<? extends Annotation> scopeType) {
        // Application/Singleton context is always active
        if (ApplicationScoped.class.equals(scopeType)) {
            return applicationContext;
        } else if (Singleton.class.equals(scopeType)) {
            return singletonContext;
        }
        Collection<InjectableContext> contextsForScope = contexts.get(scopeType);
        InjectableContext selected = null;
        if (contextsForScope != null) {
            for (InjectableContext context : contextsForScope) {
                if (context.isActive()) {
                    if (selected != null) {
                        throw new IllegalArgumentException(
                                "More than one context object for the given scope: " + selected + " " + context);
                    }
                    selected = context;
                }
            }
        }
        return selected;
    }

    @Override
    public List<InjectableContext> getContexts(Class<? extends Annotation> scopeType) {
        return contexts.getOrDefault(scopeType, Collections.emptyList());
    }

    @Override
    public Set<Class<? extends Annotation>> getScopes() {
        return new HashSet<>(contexts.keySet());
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> Supplier<InstanceHandle<T>> instanceSupplier(Class<T> type, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        }
        Set<InjectableBean<?>> resolvedBeans = resolved.getValue(new Resolvable(type, qualifiers));
        Set<InjectableBean<?>> filteredBean = resolvedBeans;
        if (resolvedBeans.size() > 1) {
            //if there are multiple beans we look for an exact match
            //this method is only called with the exact type required
            //so ignoring subclasses is the correct behaviour
            filteredBean = new HashSet<>();
            for (InjectableBean<?> i : resolvedBeans) {
                if (i.getBeanClass().equals(type)) {
                    filteredBean.add(i);
                }
            }
        }
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
        return requestContext;
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
    public String toString() {
        return "ArcContainerImpl [id=" + id + ", running=" + running + ", beans=" + beans.size() + ", observers="
                + observers.size() + ", scopes="
                + contexts.size() + "]";
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
            requestContext.terminate();
            // Fire an event with qualifier @BeforeDestroyed(ApplicationScoped.class)
            Set<Annotation> beforeDestroyQualifiers = new HashSet<>(4);
            beforeDestroyQualifiers.add(BeforeDestroyed.Literal.APPLICATION);
            beforeDestroyQualifiers.add(Any.Literal.INSTANCE);
            try {
                EventImpl.createNotifier(Object.class, Object.class, beforeDestroyQualifiers, this, false).notify(toString());
            } catch (Exception e) {
                LOGGER.warn("An error occurred during delivery of the @BeforeDestroyed(ApplicationScoped.class) event", e);
            }
            // Destroy contexts
            applicationContext.destroy();
            // Fire an event with qualifier @Destroyed(ApplicationScoped.class)
            Set<Annotation> destroyQualifiers = new HashSet<>(4);
            destroyQualifiers.add(Destroyed.Literal.APPLICATION);
            destroyQualifiers.add(Any.Literal.INSTANCE);
            try {
                EventImpl.createNotifier(Object.class, Object.class, destroyQualifiers, this, false).notify(toString());
            } catch (Exception e) {
                LOGGER.warn("An error occurred during delivery of the @Destroyed(ApplicationScoped.class) event", e);
            }
            singletonContext.destroy();

            // Clear caches
            Reflections.clearCaches();
            contexts.clear();
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
        return removedBeans;
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

    private <T> InstanceHandle<T> instanceHandle(Type type, Annotation... qualifiers) {
        return beanInstanceHandle(getBean(type, qualifiers), null);
    }

    static <T> InstanceHandle<T> beanInstanceHandle(InjectableBean<T> bean, CreationalContextImpl<T> parentContext,
            boolean resetCurrentInjectionPoint, Consumer<T> destroyLogic) {
        if (bean != null) {
            if (parentContext == null && Dependent.class.equals(bean.getScope())) {
                parentContext = new CreationalContextImpl<>(null);
            }
            CreationalContextImpl<T> creationalContext = parentContext != null ? parentContext.child(bean)
                    : new CreationalContextImpl<>(bean);
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

    <T> InstanceHandle<T> beanInstanceHandle(InjectableBean<T> bean, CreationalContextImpl<T> parentContext) {
        return beanInstanceHandle(bean, parentContext, true, null);
    }

    @SuppressWarnings("unchecked")
    private <T> InjectableBean<T> getBean(Type requiredType, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        } else {
            Qualifiers.verify(qualifiers, qualifierNonbindingMembers.keySet());
        }
        Set<InjectableBean<?>> resolvedBeans = resolved.getValue(new Resolvable(requiredType, qualifiers));
        return resolvedBeans.size() != 1 ? null : (InjectableBean<T>) resolvedBeans.iterator().next();
    }

    Set<Bean<?>> getBeans(Type requiredType, Annotation... qualifiers) {
        if (requiredType instanceof TypeVariable) {
            throw new IllegalArgumentException("The given type is a type variable: " + requiredType);
        }
        Qualifiers.verify(qualifiers, qualifierNonbindingMembers.keySet());
        // This method does not cache the results
        return Set.of(getMatchingBeans(new Resolvable(requiredType, qualifiers)).toArray(new Bean<?>[] {}));
    }

    Set<Bean<?>> getBeans(String name) {
        // This method does not cache the results
        return new HashSet<>(getMatchingBeans(name));
    }

    Map<Class<? extends Annotation>, Set<Annotation>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
    }

    Set<String> getCustomQualifiers() {
        return qualifierNonbindingMembers.keySet();
    }

    boolean isScope(Class<? extends Annotation> annotationType) {
        if (annotationType.isAnnotationPresent(Scope.class) || annotationType.isAnnotationPresent(NormalScope.class)) {
            return true;
        }
        for (Class<? extends Annotation> scopeType : contexts.keySet()) {
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
        Collection<InjectableContext> injectableContexts = contexts.get(annotationType);
        if (injectableContexts != null) {
            for (InjectableContext context : injectableContexts) {
                if (context.isNormal()) {
                    return true;
                }
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
        if (matching.isEmpty() && !removedBeans.isEmpty()) {
            List<RemovedBean> removedMatching = new ArrayList<>();
            for (RemovedBean removedBean : removedBeans) {
                if (matches(removedBean.getTypes(), removedBean.getQualifiers(), resolvable.requiredType,
                        resolvable.qualifiers)) {
                    removedMatching.add(removedBean);
                }
            }
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
        return matching;
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
        Qualifiers.verify(eventQualifiers, qualifierNonbindingMembers.keySet());
        if (observers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Type> eventTypes = new HierarchyDiscovery(eventType).getTypeClosure();
        List<InjectableObserverMethod<? super T>> resolvedObservers = new ArrayList<>();
        for (InjectableObserverMethod<?> observer : observers) {
            if (EventTypeAssignabilityRules.instance().matches(observer.getObservedType(), eventTypes)) {
                if (observer.getObservedQualifiers().isEmpty()
                        || Qualifiers.isSubset(observer.getObservedQualifiers(), eventQualifiers, qualifierNonbindingMembers)) {
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
        List<Interceptor<?>> interceptors = new ArrayList<>();
        List<Annotation> bindings = new ArrayList<>();
        for (Annotation binding : interceptorBindings) {
            bindings.add(binding);
            Set<Annotation> transitive = transitiveInterceptorBindings.get(binding.annotationType());
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
        List<Decorator<?>> decorators = new ArrayList<>();
        for (InjectableDecorator<?> decorator : this.decorators) {
            if (decoratorMatches(types, Set.of(qualifiers), decorator.getDelegateType(),
                    decorator.getDelegateQualifiers().toArray(new Annotation[] {}))) {
                decorators.add(decorator);
            }
        }
        return decorators;
    }

    private boolean hasAllInterceptionBindings(InjectableInterceptor<?> interceptor, Iterable<Annotation> bindings) {
        // The method or constructor has all the interceptor bindings of the interceptor
        for (Annotation binding : interceptor.getInterceptorBindings()) {
            // The resolution rules are the same for qualifiers
            if (!Qualifiers.hasQualifier(bindings, binding, qualifierNonbindingMembers)) {
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
        return Qualifiers.hasQualifiers(beanQualifiers, qualifierNonbindingMembers, qualifiers);
    }

    private boolean decoratorMatches(Set<Type> beanTypes, Set<Annotation> beanQualifiers, Type delegateType,
            Annotation... delegateQualifiers) {
        if (!DelegateInjectionPointAssignabilityRules.instance().matches(delegateType, beanTypes)) {
            return false;
        }
        return Qualifiers.hasQualifiers(beanQualifiers, qualifierNonbindingMembers, delegateQualifiers);
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
            // if the type is Event or Instance (the built-in types), the resolution simplifies type to raw type and ignores qualifiers
            // this is so that every injection point matches the bean we provide for that type
            Type rawType = Reflections.getRawType(requiredType);
            if (Event.class.equals(rawType) || Instance.class.equals(rawType)) {
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
