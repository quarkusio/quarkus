package io.quarkus.arc;

/**
 * A factory for proxies that perform {@link jakarta.interceptor.AroundInvoke AroundInvoke}
 * interception before forwarding the method call to the target instance ({@code delegate}).
 * <p>
 * This construct only supports {@code @AroundInvoke} interceptors declared on
 * interceptor classes; other kinds of interception, as well as {@code @AroundInvoke}
 * interceptors declared on the target class and its superclasses, are not supported.
 * <p>
 * The container provides a built-in bean with scope {@link jakarta.enterprise.context.Dependent Dependent}
 * and qualifier {@link jakarta.enterprise.inject.Default Default} that can be injected
 * into a method parameter of a producer method. Synthetic bean creation function can
 * obtain an instance by calling {@link SyntheticCreationalContext#getInterceptionProxy()}.
 * The type argument {@code T} must be equal to the return type of the producer method
 * or the provider type of the synthetic bean.
 *
 * <pre>
 * &#64;Produces
 * public MyClass produce(InterceptionProxy&lt;MyClass&gt; proxy) {
 *     return proxy.create(new MyClass());
 * }
 * </pre>
 *
 * By default, interceptor binding annotations are obtained from the target class (that is,
 * the class of the return type of the producer method or the provider type of the synthetic
 * bean). If you want to override that, use {@link BindingsSource}.
 *
 * @param <T> the type of the target instance (for which the proxy is created)
 */
public interface InterceptionProxy<T> {
    /**
     * Creates a proxy that wraps given {@code delegate} and performs interception
     * before forwarding the method call to the target instance.
     *
     * @param delegate the target instance
     * @return the interception proxy
     */
    T create(T delegate);
}
