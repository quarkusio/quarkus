package io.quarkus.arc.processor;

import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Builder of {@link Invoker}s that allows configuring input lookups, input and output
 * transformations, and invoker wrapping. The method for which the invoker is built is
 * called the <em>target method</em>. If a lookup is configured, the corresponding input
 * of the invoker is ignored and an instance is looked up from the CDI container before
 * the target method is invoked. If a transformation is configured, the corresponding input
 * or output of the invoker is modified in certain way before or after the target method
 * is invoked. If a wrapper is configured, the invoker is passed to custom code for getting
 * invoked. As a result, the built {@code Invoker} instance may have more complex behavior
 * than just directly calling the target method.
 * <p>
 * Transformations and wrapping are expressed by ordinary methods that must have
 * a pre-defined signature, as described below. Such methods are called
 * <em>transformers</em> and <em>wrappers</em>.
 * <p>
 * Invokers may only be built during deployment. It is not possible to build new invokers
 * at application runtime.
 * <p>
 * <strong>This documentation only covers what is not covered by the CDI specification.
 * See the CDI specification, section <em>Method Invokers</em>, first.</strong>
 *
 * <h2>Example</h2>
 *
 * Before describing in detail how transformers and wrappers work, let's take a look at
 * an example. Say we have the following bean with a method:
 *
 * <pre>
 * class MyService {
 *     String hello(String name) {
 *         return "Hello " + name + "!";
 *     }
 * }
 * </pre>
 *
 * And we want to build an invoker that looks up {@code MyService} from the CDI container,
 * always passes the argument to {@code hello()} as all upper-case, and repeats the return
 * value twice. To transform the argument, we can use the zero-parameter method
 * {@code String.toUpperCase()}, and to transform the return value, we write a transformer
 * as a simple {@code static} method:
 *
 * <pre>
 * class Transformations {
 *     static String repeatTwice(String str) {
 *         return str + " " + str;
 *     }
 * }
 * </pre>
 *
 * Then, assuming we have obtained the {@code InvokerBuilder} for {@code MyService.hello()},
 * we can set up the lookup and transformations and build an invoker like so:
 *
 * <pre>
 * builder.withInstanceLookup()
 *         .withArgumentTransformer(0, String.class, "toUpperCase")
 *         .withReturnValueTransformer(Transformations.class, "repeatTwice")
 *         .build();
 * </pre>
 *
 * The resulting invoker will be equivalent to the following class:
 *
 * <pre>
 * class TheInvoker implements Invoker&lt;MyService, String&gt; {
 *     String invoke(MyService ignored, Object[] arguments) {
 *         MyService instance = CDI.current().select(MyService.class).get();
 *         String argument = (String) arguments[0];
 *         String transformedArgument = argument.toUpperCase();
 *         String result = instance.hello(transformedArgument);
 *         String transformedResult = Transformations.repeatTwice(result);
 *         return transformedResult;
 *     }
 * }
 * </pre>
 *
 * The caller of this invoker may pass {@code null} as the target instance, because
 * the invoker will lookup the target instance on its own. Therefore, calling
 * {@code invoker.invoke(null, new Object[] {"world"})} will return
 * {@code "Hello WORLD! Hello WORLD!"}.
 *
 * <h2>General requirements</h2>
 *
 * To refer to a transformer or a wrapper, all methods in this builder accept:
 * 1. the {@code Class} that declares the method, and 2. the {@code String} name
 * of the method.
 * <p>
 * Transformers may be {@code static}, in which case they must be declared directly
 * on the given class, or they may be instance methods, in which case they may be declared
 * on the given class or inherited from any of its supertypes.
 * <p>
 * It is possible to register only one transformer of each kind, or for each argument
 * position in case of argument transformers. Attempting to register a second transformer
 * of the same kind, or for the same argument position, leads to an exception.
 * <p>
 * Wrappers must be {@code static} and must be declared directly on the given class.
 * It is possible to register only one wrapper. Attempting to register a second wrapper
 * leads to an exception.
 * <p>
 * It is a deployment problem if no method with given name and valid signature is found,
 * or if multiple methods with given name and different valid signatures are found. It is
 * a deployment problem if a registered transformer or wrapper is not {@code public}.
 * <p>
 * Transformers and wrappers may declare the {@code throws} clause. The declared exception
 * types are ignored when searching for the method.
 * <p>
 * For the purpose of the specification of transformers and wrappers below, the term
 * <em>any-type</em> is recursively defined as: the {@code java.lang.Object} class type,
 * or a type variable that has no bound, or a type variable whose first bound is
 * <em>any-type</em>.
 *
 * <h2>Input transformations</h2>
 *
 * The target method has 2 kinds of inputs: the target instance (unless the target method is
 * {@code static}, in which case the target instance is ignored and should be {@code null}
 * by convention) and arguments. These inputs correspond to the parameters of
 * {@link Invoker#invoke(Object, Object[]) Invoker.invoke()}.
 * <p>
 * Each input can be transformed by a transformer that has one of the following signatures,
 * where {@code X} and {@code Y} are types:
 *
 * <ul>
 * <li>{@code static X transform(Y value)}</li>
 * <li>{@code static X transform(Y value, Consumer<Runnable> cleanup)}</li>
 * <li>{@code X transform()} &ndash; in this case, {@code Y} is the type of the class that
 * declares the transformer</li>
 * </ul>
 *
 * An input transformer must produce a type that can be consumed by the target method.
 * Specifically: when {@code X} is <em>any-type</em>, it is not type checked during deployment.
 * Otherwise, it is a deployment problem if {@code X} is not assignable to the corresponding type
 * in the declaration of the target method (that is the bean class in case of target instance
 * transformers, or the corresponding parameter type in case of argument transformers). {@code Y}
 * is not type checked during deployment, so that input transformers may consume arbitrary types.
 * <p>
 * When a transformer is registered for given input, it is called before the target method is
 * invoked, and the outcome of the transformer is used in the invocation instead of the original
 * value passed to the invoker by its caller.
 * <p>
 * If the transformer declares the {@code Consumer<Runnable>} parameter, and the execution
 * of the transformer calls {@code Consumer.accept()} with some {@code Runnable}, it is
 * guaranteed that the {@code Runnable} will be called after the target method is invoked but
 * before the invoker returns. These {@code Runnable}s are called <em>cleanup tasks</em>.
 * The order of cleanup task execution is not specified. Passing a {@code null} cleanup task
 * to the {@code Consumer} is permitted, but has no effect.
 * <p>
 * If an input transformation is configured for an input for which a lookup is also configured,
 * the lookup is performed first and the transformation is applied to the looked up value.
 * If the looked up bean for some input is {@code @Dependent}, it is guaranteed that all
 * cleanup tasks registered by a transformer for that input are called before that looked up
 * {@code @Dependent} bean is destroyed.
 * <p>
 * The order in which input transformations are performed in not specified and must not
 * be relied upon.
 *
 * <h2>Output transformations</h2>
 *
 * The target method has 2 kinds of outputs: the return value and the thrown exception. These
 * outputs correspond to the return value of {@link Invoker#invoke(Object, Object[]) Invoker.invoke()}
 * or its thrown exception, respectively.
 * <p>
 * Each output can be transformed by a transformer that has one of the following signatures,
 * where {@code X} and {@code Y} are types:
 *
 * <ul>
 * <li>{@code static X transform(Y value)}</li>
 * <li>{@code X transform()} &ndash; in this case, {@code Y} is the type of the class that
 * declares the transformer</li>
 * </ul>
 *
 * An output transformer must consume a type that can be produced by the target method.
 * Specifically: when {@code Y} is <em>any-type</em>, it is not type checked during deployment.
 * Otherwise, it is a deployment problem if {@code Y} is not assignable from the return type of
 * the target method in case of return value transformers, or from {@code java.lang.Throwable}
 * in case of exception transformers. {@code X} is not type checked during deployment, so that
 * output transformers may produce arbitrary types.
 * <p>
 * When a transformer is registered for given output, it is called after the target method
 * is invoked, and the outcome of the transformer is passed back to the caller of the invoker
 * instead of the original output produced by the target method.
 * <p>
 * If the target method returns normally, any registered exception transformer is ignored; only
 * the return value transformer is called. The return value transformer may throw, in which case
 * the invoker will rethrow the exception. If the invoker is supposed to return normally,
 * the return value transformer must return normally.
 * <p>
 * Similarly, if the target method throws, any registered return value transformer is ignored;
 * only the exception transformer is called. The exception transformer may return normally,
 * in which case the invoker will return the return value of the exception transformer. If
 * the invoker is supposed to throw an exception, the exception transformer must throw.
 *
 * <h2>Invoker wrapping</h2>
 *
 * An invoker, possibly utilizing input lookups and input/output transformations, may be wrapped
 * by a custom piece of code for maximum flexibility. A wrapper must have the following signature,
 * where {@code X}, {@code Y} and {@code Z} are types:
 *
 * <ul>
 * <li>{@code static Z wrap(X instance, Object[] arguments, Invoker<X, Y> invoker)}</li>
 * </ul>
 *
 * A wrapper must operate on a matching instance type. Specifically: when {@code X} is
 * <em>any-type</em>, it is not type checked during deployment. Otherwise, it is a deployment
 * problem if {@code X} is not assignable from the class type of the bean class to which
 * the target method belongs. {@code Y} and {@code Z} are not type checked during deployment.
 * <p>
 * When a wrapper is registered, 2 invokers for the same method are created. The <em>inner</em>
 * invoker applies all lookups and transformations, as described in previous sections, and
 * invokes the target method. The <em>outer</em> invoker calls the wrapper with the passed
 * instance and arguments and an instance of the inner invoker. The outer invoker is returned
 * by this invoker builder.
 * <p>
 * In other words, the outer invoker is equivalent to the following class:
 *
 * <pre>
 * class InvokerWrapper implements Invoker&lt;X, Z&gt; {
 *     Z invoke(X instance, Object[] arguments) {
 *         // obtain the invoker as if no wrapper existed
 *         Invoker&lt;X, Y&gt; invoker = obtainInvoker();
 *         return SomeClass.wrap(instance, arguments, invoker);
 *     }
 * }
 * </pre>
 *
 * If the wrapper returns normally, the outer invoker returns its return value, unless the wrapper
 * is declared {@code void}, in which case the outer invoker returns {@code null}. If the wrapper
 * throws an exception, the outer invoker rethrows it directly.
 * <p>
 * The wrapper is supposed to call the invoker it is passed, but does not necessarily have to.
 * The wrapper may call the invoker multiple times. The wrapper must not use the invoker
 * in any other way; specifically, it is forbidden to store the invoker instance anywhere
 * or pass it to other methods that do not follow these rules. Doing so leads to non-portable
 * behavior.
 *
 * <h2>Type checking</h2>
 *
 * An invoker created by this builder has relaxed type checking rules, when compared to
 * the description in {@link Invoker#invoke(Object, Object[]) Invoker.invoke()}, depending
 * on configured lookups, transformers and wrapper. Some types are checked during
 * deployment, as described in previous sections. Other types are checked during invocation,
 * at the very least due to the type checks performed implicitly by the JVM. The lookups,
 * transformers and the wrapper must arrange the inputs and outputs so that when the method
 * is eventually invoked, the rules described in the CDI specification all hold.
 */
public class InvokerBuilder {
    final BeanInfo targetBean;
    final ClassInfo targetBeanClass;
    final MethodInfo targetMethod;

    boolean instanceLookup;
    boolean[] argumentLookups;

    InvocationTransformer instanceTransformer;
    InvocationTransformer[] argumentTransformers;
    InvocationTransformer returnValueTransformer;
    InvocationTransformer exceptionTransformer;

    InvocationTransformer invocationWrapper;

    private final BeanDeployment beanDeployment;
    private final InjectionPointModifier injectionPointTransformer;

    InvokerBuilder(BeanInfo targetBean, MethodInfo targetMethod, BeanDeployment beanDeployment,
            InjectionPointModifier injectionPointTransformer) {
        this.targetBean = targetBean;
        this.targetBeanClass = targetBean.getImplClazz();
        this.targetMethod = targetMethod;

        this.argumentTransformers = new InvocationTransformer[targetMethod.parametersCount()];
        this.argumentLookups = new boolean[targetMethod.parametersCount()];

        this.beanDeployment = beanDeployment;
        this.injectionPointTransformer = injectionPointTransformer;
    }

    /**
     * Enables lookup of the target instance.
     *
     * @return this builder
     */
    public InvokerBuilder withInstanceLookup() {
        instanceLookup = true;
        return this;
    }

    /**
     * Enables lookup of the argument on given {@code position}.
     *
     * @param position zero-based argument position for which lookup is enabled
     * @return this builder
     * @throws IllegalArgumentException if {@code position} is less than zero or greater than
     *         or equal to the number of parameters declared by the target method
     */
    public InvokerBuilder withArgumentLookup(int position) {
        if (position < 0 || position >= argumentLookups.length) {
            throw new IllegalArgumentException("Position must be >= 0 && < " + argumentLookups.length);
        }

        argumentLookups[position] = true;
        return this;
    }

    /**
     * Configures an input transformer for the target instance.
     *
     * @param clazz class that declares the transformer
     * @param methodName transformer method name
     * @return this builder
     * @throws IllegalStateException if this method is called more than once
     */
    public InvokerBuilder withInstanceTransformer(Class<?> clazz, String methodName) {
        if (instanceTransformer != null) {
            throw new IllegalStateException("Instance transformer already set");
        }

        this.instanceTransformer = new InvocationTransformer(InvocationTransformerKind.INSTANCE, clazz, methodName);
        return this;
    }

    /**
     * Configures an input transformer for the argument on given {@code position}.
     *
     * @param position zero-based argument position for which the input transformer is configured
     * @param clazz class that declares the transformer
     * @param methodName transformer method name
     * @return this builder
     * @throws IllegalArgumentException if {@code position} is less than zero or greater than
     *         or equal to the number of parameters declared by the target method
     * @throws IllegalStateException if this method is called more than once with the same {@code position}
     */
    public InvokerBuilder withArgumentTransformer(int position, Class<?> clazz, String methodName) {
        if (position < 0 || position >= argumentTransformers.length) {
            throw new IllegalArgumentException("Position must be >= 0 && < " + argumentTransformers.length);
        }
        if (argumentTransformers[position] != null) {
            throw new IllegalStateException("Argument transformer already set for position " + position);
        }

        this.argumentTransformers[position] = new InvocationTransformer(InvocationTransformerKind.ARGUMENT, clazz, methodName);
        return this;
    }

    /**
     * Configures an output transformer for the return value.
     *
     * @param clazz class that declares the transformer
     * @param methodName transformer method name
     * @return this builder
     * @throws IllegalStateException if this method is called more than once
     */
    public InvokerBuilder withReturnValueTransformer(Class<?> clazz, String methodName) {
        if (returnValueTransformer != null) {
            throw new IllegalStateException("Return value transformer already set");
        }

        this.returnValueTransformer = new InvocationTransformer(InvocationTransformerKind.RETURN_VALUE, clazz, methodName);
        return this;
    }

    /**
     * Configures an output transformer for the thrown exception.
     *
     * @param clazz class that declares the transformer
     * @param methodName transformer method name
     * @return this builder
     * @throws IllegalStateException if this method is called more than once
     */
    public InvokerBuilder withExceptionTransformer(Class<?> clazz, String methodName) {
        if (exceptionTransformer != null) {
            throw new IllegalStateException("Exception transformer already set");
        }

        this.exceptionTransformer = new InvocationTransformer(InvocationTransformerKind.EXCEPTION, clazz, methodName);
        return this;
    }

    /**
     * Configures an invoker wrapper.
     *
     * @param clazz class that declares the invoker wrapper
     * @param methodName invoker wrapper method name
     * @return this builder
     * @throws IllegalStateException if this method is called more than once
     */
    public InvokerBuilder withInvocationWrapper(Class<?> clazz, String methodName) {
        if (invocationWrapper != null) {
            throw new IllegalStateException("Invocation wrapper already set");
        }

        this.invocationWrapper = new InvocationTransformer(InvocationTransformerKind.WRAPPER, clazz, methodName);
        return this;
    }

    /**
     * Returns a representation of the built {@link Invoker}.
     *
     * @return the built invoker
     */
    public InvokerInfo build() {
        Injection argumentsInjection = Injection.forInvokerArgumentLookups(targetBean.getImplClazz(), targetMethod,
                argumentLookups, beanDeployment, injectionPointTransformer);
        InvokerInfo result = new InvokerInfo(this, argumentsInjection, beanDeployment);

        beanDeployment.addInvoker(result);
        return result;
    }
}
