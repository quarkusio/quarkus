package org.jboss.shamrock.deployment;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.protean.gizmo.MethodCreator;
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
     *
     * This will add all constructors, as well as all methods and fields if the appropriate fields are set.
     *
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
     *
     * @param applicationClass If this class should be loaded by the application class loader when in runtime mode
     * @param name The class name
     * @param classData The class bytes
     * @throws IOException
     */
    void addGeneratedClass(boolean applicationClass, String name, byte[] classData) throws IOException;

    /**
     * Creates a resources with the provided contents
     * @param name
     * @param data
     * @throws IOException
     */
    void createResource(String name, byte[] data) throws IOException;

    /**
     * Adds a bytecode transformer that can transform application classes.
     * <p>
     * This takes the form of a function that takes a string, and returns an ASM visitor, or null if transformation
     * is not required.
     *
     * At present these transformations are only applied to application classes, not classes provided by dependencies
     */
    void addByteCodeTransformer(Function<String, Function<ClassVisitor, ClassVisitor>> visitorFunction);

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
    void addRuntimeInitializedClasses(String ... classes);

    /**
     * Adds a proxy definition to allow proxies to be created using {@link java.lang.reflect.Proxy}
     *
     * @param proxyClasses The interface names that this proxy will implement
     */
    void addProxyDefinition(String ... proxyClasses);

    /**
     *
     * @param capability
     * @return
     */
    boolean isCapabilityPresent(String capability);

    <T> void setProperty(String key, T value);

    <T> T getProperty(String key);
}
