package org.jboss.shamrock.deployment;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.objectweb.asm.ClassVisitor;

/**
 * Interface that represents the current processor state. This is basically the output context, processors can use it
 * to generate bytecode or provide additional information.
 */
public interface ProcessorContext {


    /**
     * Adds a new static init task with the given priority. This task will be from a static init
     * block in priority order
     * <p>
     * These tasks are always run before deployment tasks
     *
     * @param priority The priority
     * @return A recorder than can be used to generate bytecode
     */
    BytecodeRecorder addStaticInitTask(int priority);

    /**
     * Adds a new deployment task with the given priority. This task will be run on startup in priority order.
     *
     * @param priority The priority
     * @return A recorder than can be used to generate bytecode
     */
    BytecodeRecorder addDeploymentTask(int priority);

    /**
     * This method is used to indicate that a given class requires reflection.
     * <p>
     * It is used in the graal output to allow the class to be reflected when running on substrate VM
     * <p>
     * This will add all constructors, as well as all methods and fields if the appropriate fields are set.
     * <p>
     * Where possible consider using the more fine grained addReflective* variants
     *
     * @param className The class name
     */
    void addReflectiveClass(boolean methods, boolean fields, String... className);

    void addReflectiveField(FieldInfo fieldInfo);

    void addReflectiveField(Field fieldInfo);

    void addReflectiveMethod(MethodInfo methodInfo);

    void addReflectiveMethod(Method methodInfo);

    /**
     * Attempts to register a complete type hierarchy for reflection.
     * <p>
     * This is intended to be used to register types that are going to be serialized,
     * e.g. by Jackson or some other JSON mapper.
     * <p>
     * This will do 'smart discovery' and in addition to registering the type itself it will also attempt to
     * register the following:
     * <p>
     * - Superclasses
     * - Component types of collections
     * - Types used in bean properties if (if method reflection is enabled)
     * - Field types (if field reflection is enabled)
     * <p>
     * This discovery is applied recursively, so any additional types that are registered will also have their dependencies
     * discovered
     */
    void addReflectiveHierarchy(Type type);


    /**
     * @param applicationClass If this class should be loaded by the application class loader when in runtime mode
     * @param name             The class name
     * @param classData        The class bytes
     * @throws IOException
     */
    void addGeneratedClass(boolean applicationClass, String name, byte[] classData) throws IOException;

    /**
     * Creates a resources with the provided contents
     *
     * @param name
     * @param data
     * @throws IOException
     */
    void createResource(String name, byte[] data) throws IOException;

    /**
     * Adds a bytecode transformer that can transform application classes
     * <p>
     * This is added on a per-class basis, by specifying the class name. The transformer is a function that
     * can be used to wrap an ASM {@link ClassVisitor}.
     * <p>
     * The transformation is applied by calling each function that has been registered it turn to create a chain
     * of visitors. These visitors are then applied and the result is saved to the output.
     * <p>
     * At present these transformations are only applied to application classes, not classes provided by dependencies
     * <p>
     * These transformations may be run concurrently in multiple threads, so if a function is registered for multiple
     * classes it must be thread safe
     */
    void addByteCodeTransformer(String classToTransform, BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction);

    /**
     * Adds a resource to the image that will be accessible when running under substrate.
     *
     * @param name The resource path
     */
    void addResource(String name);

    void addResourceBundle(String bundle);

    /**
     * Marks a class as being runtime initialized, which means that running the static
     * initializer will happen at runtime
     *
     * @param classes The classes to lazily init
     */
    void addRuntimeInitializedClasses(String... classes);

    /**
     * Adds a proxy definition to allow proxies to be created using {@link java.lang.reflect.Proxy}
     *
     * @param proxyClasses The interface names that this proxy will implement
     */
    void addProxyDefinition(String... proxyClasses);

    /**
     * Set a system property to be passed in to the native image tool.
     *
     * @param name  the property name (must not be {@code null})
     * @param value the property value
     */
    void addNativeImageSystemProperty(String name, String value);

    /**
     * @param capability
     * @return if the given capability is present
     * @see Capabilities
     */
    boolean isCapabilityPresent(String capability);

    <T> void setProperty(String key, T value);

    <T> T getProperty(String key);
}
