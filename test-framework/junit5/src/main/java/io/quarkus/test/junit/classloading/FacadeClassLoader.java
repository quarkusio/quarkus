package io.quarkus.test.junit.classloading;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.support.AnnotationSupport;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.test.junit.AppMakerHelper;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.TestResourceUtil;

/**
 * JUnit has many interceptors and listeners, but it does not allow us to intercept test discovery in a fine-grained way that
 * would allow us to swap the thread context classloader.
 * Since we can't intercept with a JUnit hook, we hijack from inside the classloader.
 * <p>
 * We need to load all our test classes in one go, during the discovery phase, before we start the applications.
 * We may need several applications and therefore, several classloaders, depending on what profiles are set.
 * To solve that, we prepare the applications, to get classloaders, and file them here.
 */
public class FacadeClassLoader extends ClassLoader implements Closeable {
    private static final Logger log = Logger.getLogger(FacadeClassLoader.class);
    protected static final String JAVA = "java.";

    private static final String NAME = "FacadeLoader";
    private static final String IO_QUARKUS_TEST_JUNIT_QUARKUS_TEST_EXTENSION = "io.quarkus.test.junit.QuarkusTestExtension";
    public static final String VALUE = "value";
    public static final String KEY_PREFIX = "QuarkusTest-";
    public static final String DISPLAY_NAME_PREFIX = "JUnit";
    // TODO it would be nice, and maybe theoretically possible, to re-use the curated application?
    // TODO and if we don't, how do we get a re-usable deployment classloader?

    // TODO does this need to be a thread safe maps?
    private final Map<String, CuratedApplication> curatedApplications = new HashMap<>();
    private final Map<String, StartupAction> runtimeClassLoaders = new HashMap<>();
    private static final String NO_PROFILE = "no-profile";

    /*
     * It seems kind of wasteful to load every class twice; that's true, but it's been the case (by a different mechanism)
     * ever since Quarkus 1.2 and the move to isolated classloaders, because the test extension would reload classes into the
     * runtime classloader.
     * In the future, https://openjdk.org/jeps/466 would allow us to avoid inspecting the classes to avoid a double load in the
     * delegating
     * classloader
     * The solution referenced by
     * https://github.com/junit-team/junit5/discussions/4203,https://github.com/marcphilipp/gradle-sandbox/blob/
     * baaa1972e939f5817f54a3d287611cef0601a58d/classloader-per-test-class/src/test/java/org/example/
     * ClassLoaderReplacingLauncherSessionListener.java#L23-L44
     * does use a similar approach, although they have a default loader rather than a canary loader.
     */
    private final URLClassLoader peekingClassLoader;

    // Ideally these would be final, but we initialise them in a try-catch block and sometimes they will be caught
    private Class<? extends Annotation> quarkusTestAnnotation;
    private Class<? extends Annotation> disabledAnnotation;
    private Class<? extends Annotation> quarkusIntegrationTestAnnotation;
    private Class<? extends Annotation> profileAnnotation;
    private Class<? extends Annotation> extendWithAnnotation;
    private Class<? extends Annotation> registerExtensionAnnotation;
    private final Map<String, Class<?>> profiles;
    private final Set<String> quarkusTestClasses;
    private final boolean isAuxiliaryApplication;
    private QuarkusClassLoader keyMakerClassLoader;

    private static volatile FacadeClassLoader instance;

    public static void clearSingleton() {
        if (instance != null) {
            instance.close();
        }
        instance = null;
    }

    // We don't ever want more than one FacadeClassLoader active, especially since config gets initialised on it.
    // The gradle test execution can make more than one, perhaps because of its threading model.
    public static FacadeClassLoader instance(ClassLoader parent) {
        if (instance == null) {
            instance = new FacadeClassLoader(parent);
        }
        return instance;
    }

    public static FacadeClassLoader instance(ClassLoader parent, boolean isAuxiliaryApplication, Map<String, String> profiles,
            Set<String> quarkusTestClasses, String... classesPath) {
        if (instance == null) {
            instance = new FacadeClassLoader(parent, isAuxiliaryApplication, profiles, quarkusTestClasses,
                    String.join(File.pathSeparator, classesPath));
        }
        return instance;
    }

    public FacadeClassLoader(ClassLoader parent) {
        // TODO update this commentWe need to set the super or things don't work on paths which use the maven isolated classloader, such as google cloud functions tests
        // It seems something in that path is using a method other than loadClass(), and so the inherited method can't do the right thing without a parent
        // TODO if this is launched with a launcher, java.class.path may not be correct - see https://maven.apache.org/surefire/maven-surefire-plugin/examples/class-loading.html
        // TODO paths with spaces in them break this - and at the moment, no test catches that

        this(parent, false, null, null, System.getProperty("java.class.path"));
    }

    public FacadeClassLoader(ClassLoader parent, boolean isAuxiliaryApplication, Map<String, String> profileNames,
            Set<String> quarkusTestClasses,
            String classesPath) {
        super(parent);
        this.quarkusTestClasses = quarkusTestClasses;
        this.isAuxiliaryApplication = isAuxiliaryApplication;

        URL[] urls = Arrays.stream(classesPath.split(File.pathSeparator))
                .map(spec -> {
                    try {
                        // This manipulation is needed to work in IDEs
                        if (!spec.endsWith("jar") && !spec.endsWith(File.separator)) {
                            spec = spec + File.separator;
                        }

                        return Path.of(spec)
                                .toUri()
                                .toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);

        // TODO this special casing is pretty fragile and pretty ugly; without it, @WithFunction tests do not pass, such as google cloud functions
        // Tests using an isolated classloader do not work correctly with continuous testing, but this issue predates the facade classloader - see https://github.com/quarkusio/quarkus/issues/46478
        // What kind of tests use an isolated classloader? Maven will create one (and also a normal classloader) for tests with `@WithFunction`
        boolean isolatedClassloader = !(classesPath.contains(File.pathSeparator));

        // TODO can we get rid of this now that we have the guard?
        ClassLoader annotationLoader;
        if (isolatedClassloader) {
            System.out.println("HOLLY doing isolated classloader path " + classesPath);
            // If the classloader is isolated, putting the parent into the peeking classloader will just load all classes with the parent, which isn't what's wanted (and causes @WithFunction tests to fail)
            peekingClassLoader = new URLClassLoader(urls, null);
            annotationLoader = parent;
        } else {
            peekingClassLoader = new ParentLastURLClassLoader(urls, parent);
            annotationLoader = peekingClassLoader;
        }

        // In the isolated classloader case, we actually never discover any quarkus tests, and a new instance gets created;
        // but to be safe, initialise our instance variables. We can't use the peekingClassLoader because it can't see JUnit classes, so just use the parent
        try {
            extendWithAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass(ExtendWith.class.getName());
            disabledAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass(Disabled.class.getName());
            registerExtensionAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass(RegisterExtension.class.getName());
            quarkusTestAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass("io.quarkus.test.junit.QuarkusTest");
            quarkusIntegrationTestAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass(QuarkusIntegrationTest.class.getName());
            profileAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass(TestProfile.class.getName());
        } catch (ClassNotFoundException e) {
            // If QuarkusTest is not on the classpath, that's fine; it just means we definitely won't have QuarkusTests. That means we can bypass a whole bunch of logic.
            log.debug("Could not load annotations for FacadeClassLoader: " + e);
        }

        if (profileNames != null) {
            this.profiles = new HashMap<>();

            profileNames.forEach((k, profileName) -> {
                Class profile;
                if (profileName != null) {
                    try {
                        profile = peekingClassLoader.loadClass(profileName);
                    } catch (ClassNotFoundException e1) {
                        throw new RuntimeException(e1);
                    }
                    this.profiles.put(k, profile);
                }

            });
        } else {
            // We set it to null so we know not to look in it
            this.profiles = null;
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        System.out.println("HOLLY facade classloader loading " + name);
        boolean isQuarkusTest = false;
        boolean isIntegrationTest = false;
        Class<?> inspectionClass = null;

        try {

            Class<?> profile = null;
            if (profiles != null) {
                isQuarkusTest = quarkusTestClasses.contains(name);

                profile = profiles.get(name);

                // In continuous testing, only load an inspection version of the class if it's a QuarkusTest
                if (isQuarkusTest) {
                    try {
                        inspectionClass = peekingClassLoader.loadClass(name);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        return super.loadClass(name);
                    }
                }

            } else {
                if (quarkusTestAnnotation != null) {

                    // If it's not continuous testing, we need to load everything in order to decide if it's a QuarkusTest
                    try {
                        inspectionClass = peekingClassLoader.loadClass(name);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        return super.loadClass(name);
                    }

                    boolean isEnabled = !AnnotationSupport.isAnnotated(inspectionClass, disabledAnnotation);

                    // If a whole test class has an @Disabled annotation, do not bother creating a quarkus app for it
                    // Pragmatically, this fixes a LinkageError in grpc-cli which only reproduces in CI, but it's also probably what users would expect
                    if (isEnabled) {
                        // A Quarkus Test could be annotated with @QuarkusTest or with @ExtendWith[... QuarkusTestExtension.class ] or @RegisterExtension
                        // An @interface isn't a quarkus test, and doesn't want its own application; to detect it, just check if it has a superclass
                        isQuarkusTest = !inspectionClass.isAnnotation()
                                && (AnnotationSupport.isAnnotated(inspectionClass, quarkusTestAnnotation) // AnnotationSupport picks up cases where an class is annotated with an annotation which itself includes the annotation we care about
                                        || registersQuarkusTestExtensionWithExtendsWith(inspectionClass)
                                        || registersQuarkusTestExtensionOnField(inspectionClass));

                        if (isQuarkusTest) {
                            // Many integration tests have Quarkus higher up in the hierarchy, but they do not count as QuarkusTests and have to be run differently
                            isIntegrationTest = !inspectionClass.isAnnotation()
                                    && (AnnotationSupport.isAnnotated(inspectionClass, quarkusIntegrationTestAnnotation));

                            Optional<? extends Annotation> profileDeclaration = AnnotationSupport.findAnnotation(
                                    inspectionClass,
                                    profileAnnotation);
                            if (profileDeclaration.isPresent()) {

                                Method m = profileDeclaration.get()
                                        .getClass()
                                        .getMethod(VALUE);
                                // We can't be specific about what the class extends, because it's loaded with another classloader
                                profile = (Class<?>) m.invoke(profileDeclaration.get());
                            }
                        }
                    }
                }
            }

            if (isQuarkusTest && !isIntegrationTest) {

                preloadTestResourceClasses(inspectionClass);
                QuarkusClassLoader runtimeClassLoader = getQuarkusClassLoader(inspectionClass, profile);
                System.out.println("HOLLY made classloader " + runtimeClassLoader);
                Class clazz = runtimeClassLoader.loadClass(name);
                System.out.println("HOLLY did load " + clazz + " using CL " + clazz.getClassLoader());

                return clazz;
            } else {
                return super.loadClass(name);
            }

        } catch (NoSuchMethodException e) {
            // TODO better handling of these
            System.out.println("Could get method " + e);
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            System.out.println("Could not invoke " + e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            System.out.println("Could not access " + e);
            throw new RuntimeException(e);
        }

    }

    private boolean registersQuarkusTestExtensionWithExtendsWith(Class<?> inspectionClass) {

        Optional<? extends Annotation> a = AnnotationSupport.findAnnotation(inspectionClass, extendWithAnnotation);
        // This toString looks sloppy, but we cannot do an equals() because there might be multiple JUnit extensions, and getting the annotation value would need a reflective call, which may not be any cheaper or prettier than doing the string
        return (a.isPresent() && a.get().toString().contains(IO_QUARKUS_TEST_JUNIT_QUARKUS_TEST_EXTENSION));

    }

    /*
     * What's this for?
     * It's a bit like detecting the location in an privacy test or detecting the lab environment in an emissions test and then
     * deciding how to behave.
     * We're special-casing behaviour for a hard-coded selection of test packages. Yuck!
     * TODO Hopefully, once https://github.com/quarkusio/quarkus/issues/45785 is done, it will not be needed.
     * Some tests, especially in kubernetes-client and openshift-client, check config to decide whether to start a dev service.
     * That happens at augmentation, which happens before test execution.
     * In the old model, the test class would have already been loaded by JUnit first, and it would have had a chance to write
     * config to the system properties.
     * That config would influence whether dev services were started.
     * TODO even without 45785 it might be nice to find a better way, perhaps rewriting the AbstractKubernetesTestResource test
     * resource to work differently?
     *
     */
    private void preloadTestResourceClasses(Class<?> fromCanary) {
        try {
            Class<Annotation> ca = (Class<Annotation>) peekingClassLoader
                    .loadClass("io.quarkus.test.common.QuarkusTestResource");
            List<Annotation> ans = AnnotationSupport.findRepeatableAnnotations(fromCanary, ca);
            for (Annotation a : ans) {
                Method m = a
                        .getClass()
                        .getMethod(VALUE);
                Class resourceClass = (Class) m.invoke(a);
                // Only do this hack for the resources we know need it, since it can cause failures in other areas
                if (resourceClass.getName().contains("Kubernetes")) {
                    getParent().loadClass(resourceClass.getName());
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            // In some projects, these classes are not on the canary classpath. That's fine, we know there's nothing to preload.
            log.debug("Canary classloader could not preload test resources:" + e);
        }
    }

    private boolean registersQuarkusTestExtensionOnField(Class<?> inspectionClass) {

        List<Field> fields = AnnotationSupport.findAnnotatedFields(inspectionClass, registerExtensionAnnotation,
                f -> f.getType().getName()
                        .equals(IO_QUARKUS_TEST_JUNIT_QUARKUS_TEST_EXTENSION));

        return fields != null && fields.size() > 0;
    }

    private QuarkusClassLoader getQuarkusClassLoader(Class requiredTestClass, Class<?> profile) {
        final String profileName = profile != null ? profile.getName() : NO_PROFILE;
        String profileKey = KEY_PREFIX + profileName;

        try {
            StartupAction startupAction;
            String key;

            // We cannot directly access TestResourceUtil as long as we're in the core module, but the app classloaders can.
            // But, chicken-and-egg, we may not have an app classloader yet. However, if we don't, we won't need to worry about restarts, but this instance clearly cannot need a restart

            // TODO do the experiment - can it now work to use the peekingclassloader instead of the keymaker?

            // If we make a classloader with a null profile, we get the problem of starting dev services multiple times, which is very bad (if temporary) - once that issue is fixed, could reconsider
            if (keyMakerClassLoader == null) {
                // Making a classloader uses the profile key to look up a curated application
                startupAction = makeClassLoader(profileKey, requiredTestClass, profile);
                keyMakerClassLoader = startupAction.getClassLoader();

                // We cannot use the startup action one because it's a base runtime classloader and so will not have the right access to application classes (they're in its banned list)
                final String resourceKey = getResourceKey(requiredTestClass, profile);

                // The resource key might be null, and that's ok
                key = profileKey + resourceKey;
            } else {
                final String resourceKey = getResourceKey(requiredTestClass, profile);

                // The resource key might be null, and that's ok
                key = profileKey + resourceKey;
                startupAction = runtimeClassLoaders.get(key);
                if (startupAction == null) {
                    // Making a classloader uses the profile key to look up a curated application
                    startupAction = makeClassLoader(profileKey, requiredTestClass, profile);
                }

            }

            System.out.println("HOLLY With resources, key is " + key);

            // If we didn't have a classloader and didn't get a resource key
            runtimeClassLoaders.put(key, startupAction);

            return startupAction.getClassLoader();
        } catch (Exception e) {
            // Exceptions here get swallowed by the JUnit framework and we don't get any debug information unless we print it ourself
            // TODO what's the best way to do this?
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String getResourceKey(Class<?> requiredTestClass, Class profile)
            throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {

        String resourceKey;

        ClassLoader classLoader = keyMakerClassLoader;
        // We have to access TestResourceUtil reflectively, because if we used this class's classloader, it might be an augmentation classloader without access to application classes
        // TODO check this is true, try skipping reflection and also using the peeking loader
        Method method = Class
                .forName(TestResourceUtil.class.getName(), true, classLoader)
                .getMethod("getReloadGroupIdentifier", Class.class, Class.class);

        ClassLoader original = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(keyMakerClassLoader);

            // When we load the TestResourceUtil loading gets delegated to a base runtime classloader, which cannot see the app classes; so we need to pre-port the profile to its classloader before passing it to it
            Class<?> transliteratedProfile = profile != null ? keyMakerClassLoader.loadClass(profile.getName()) : null;
            // we reload the test resources (and thus the application) if we changed test class and the new test class is not a nested class, and if we had or will have per-test test resources
            resourceKey = (String) method.invoke(null, requiredTestClass, transliteratedProfile); //   TestResourceUtil.getResourcesKey(requiredTestClass);
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(original);
        }
        return resourceKey;
    }

    private StartupAction makeClassLoader(String key, Class requiredTestClass, Class profile) throws Exception {

        AppMakerHelper appMakerHelper = new AppMakerHelper();

        CuratedApplication curatedApplication = curatedApplications.get(key);

        if (curatedApplication == null) {
            Collection<Runnable> shutdownTasks = new HashSet();

            String displayName = DISPLAY_NAME_PREFIX + key;
            curatedApplication = appMakerHelper.makeCuratedApplication(requiredTestClass, displayName,
                    isAuxiliaryApplication,
                    shutdownTasks);
            curatedApplications.put(key, curatedApplication);

        }

        // TODO are all these args used?
        StartupAction startupAction = appMakerHelper.getStartupAction(requiredTestClass,
                curatedApplication, isAuxiliaryApplication, profile);

        ClassLoader original = Thread.currentThread()
                .getContextClassLoader();
        try {
            // See comments on AbstractJVMTestExtension#evaluateExecutionCondition for why this is the system classloader
            Thread.currentThread()
                    .setContextClassLoader(ClassLoader.getSystemClassLoader());

            QuarkusClassLoader loader = startupAction.getClassLoader();

            Class<?> configProviderResolverClass = loader.loadClass(ConfigProviderResolver.class.getName());

            Class<?> testConfigProviderResolverClass = loader.loadClass(QuarkusTestConfigProviderResolver.class.getName());
            Object testConfigProviderResolver = testConfigProviderResolverClass.getDeclaredConstructor(ClassLoader.class)
                    .newInstance(loader);

            configProviderResolverClass.getDeclaredMethod("setInstance", configProviderResolverClass)
                    .invoke(null,
                            testConfigProviderResolver);
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(original);
        }

        System.out.println("HOLLY at end of classload TCCL is " + Thread.currentThread().getContextClassLoader());
        return startupAction;

    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void close() {
        for (CuratedApplication curatedApplication : curatedApplications.values()) {
            curatedApplication.close();
        }
        try {

            if (peekingClassLoader != null) {
                peekingClassLoader.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
