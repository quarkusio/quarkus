package io.quarkus.test.junit.classloading;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.test.junit.AppMakerHelper;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestExtension;
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
 * <p>
 * Final, since some code does instanceof checks using the class name.
 */
public final class FacadeClassLoader extends ClassLoader implements Closeable {
    private static final Logger log = Logger.getLogger(FacadeClassLoader.class);

    private static final String NAME = "FacadeLoader";
    public static final String VALUE = "value";
    public static final String KEY_PREFIX = "QuarkusTest-";
    public static final String DISPLAY_NAME_PREFIX = "JUnit";
    // It would be nice, and maybe theoretically possible, to re-use the curated application? However, the pre-classloadingrewrite version of the codebase did not reuse curated applications between profiles,
    // which suggests it would be a major rewrite to do so. If they are re-used, most things still work, but config when there are multiple profiles does not work; see, for example, integration-tests/smallrye-config

    // We re-use curated applications across application starts; be careful what classloader this class is loaded with
    private static final Map<String, CuratedApplication> curatedApplications = new HashMap<>();

    // JUnit discovery is single threaded, so no need for concurrency on this map
    private final Map<String, QuarkusClassLoader> runtimeClassLoaders = new HashMap<>();
    private static final String NO_PROFILE = "no-profile";

    /*
     * A 'disposable' loader for holding temporary instances of the classes to allow us to inspect them.
     *
     * It seems kind of wasteful to load every class twice; that's true, but it's been the case (by a different mechanism)
     * ever since Quarkus 1.2 and the move to isolated classloaders, because the test extension would reload classes into the
     * runtime classloader.
     * In the future, https://openjdk.org/jeps/466 might allow us to avoid loading the classes to avoid a double load in the
     * delegating classloader. Whether that's actually better than using a disposable classloader + reflection, I don't know.
     * The solution referenced by
     * https://github.com/junit-team/junit5/discussions/4203,https://github.com/marcphilipp/gradle-sandbox/blob/
     * baaa1972e939f5817f54a3d287611cef0601a58d/classloader-per-test-class/src/test/java/org/example/
     * ClassLoaderReplacingLauncherSessionListener.java#L23-L44
     * does use a similar approach, although they have a default loader rather than a canary loader.
     */
    private URLClassLoader peekingClassLoader;

    // Ideally these would be final, but we initialise them in a try-catch block and sometimes they will be caught

    // JUnit extensions can be registered by a service loader - see https://junit.org/junit5/docs/current/user-guide/#extensions-registration
    private boolean isServiceLoaderMechanism;
    private Method osIsCurrent;
    private Class<? extends Annotation> quarkusTestAnnotation;
    private Class<? extends Annotation> disabledAnnotation;
    private Class<? extends Annotation> disabledOnOsAnnotation;
    private Method disabledOnOsAnnotationValue;
    private Class<? extends Annotation> quarkusIntegrationTestAnnotation;
    private Class<? extends Annotation> profileAnnotation;
    private Class<? extends Annotation> extendWithAnnotation;
    private Class<? extends Annotation> registerExtensionAnnotation;
    private Class<? extends Annotation> testAnnotation;

    // TODO maybe refactor this into a ContinuousFacadeClassLoader subclass
    private final Map<String, Class<?>> profiles;
    private final Set<String> quarkusTestClasses;
    private final boolean isAuxiliaryApplication;
    private QuarkusClassLoader keyMakerClassLoader;

    public FacadeClassLoader(ClassLoader parent) {
        this(parent, false, null, null, null, System.getProperty("java.class.path"));
    }

    public FacadeClassLoader(ClassLoader parent, boolean isAuxiliaryApplication, CuratedApplication curatedApplication,
            final Map<String, String> profileNames,
            final Set<String> quarkusTestClasses, final String classesPath) {
        super(parent);

        // Don't make a no-profile curated application, since our caller had one already
        curatedApplications.put(getProfileKey(null), curatedApplication);

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

        // If this is launched with a launcher, java.class.path may be very minimal - see https://maven.apache.org/surefire/maven-surefire-plugin/examples/class-loading.html
        // Tests using an isolated classloader do not work correctly with continuous testing, but this issue predates the facade classloader - see https://github.com/quarkusio/quarkus/issues/46478
        // What kind of tests use an isolated classloader? Maven will create one (and also a normal classloader) for tests with `@WithFunction`
        // TODO this special casing is pretty fragile and pretty ugly; without it, @WithFunction tests do not pass, such as google cloud functions
        boolean launcherClassloader = !(classesPath.contains(File.pathSeparator));

        ClassLoader annotationLoader;
        if (launcherClassloader) {
            // If the classloader is isolated, putting the parent into the peeking classloader will just load all classes with the parent, which isn't what's wanted (and causes @WithFunction tests to fail)
            peekingClassLoader = new URLClassLoader(urls, null);
            // ... but we need some way to load annotations, because they won't be visible on the classpath property
            annotationLoader = parent;
        } else {
            peekingClassLoader = new ParentLastURLClassLoader(urls, parent);
            annotationLoader = peekingClassLoader;
        }

        // In the isolated classloader case, we actually never discover any quarkus tests, and a new instance gets created;
        // but to be safe, initialise our instance variables. We can't use the peekingClassLoader because it can't see JUnit classes, so just use the parent
        // We have to use reflection because the peekingclassloader may have different versions of the JUnit classes than this class, especially when running with gradle
        try {
            extendWithAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass(ExtendWith.class.getName());
            disabledAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass(Disabled.class.getName());
            disabledOnOsAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass(DisabledOnOs.class.getName());
            Class<?> osClass = annotationLoader.loadClass(OS.class.getName());
            osIsCurrent = osClass.getMethod("isCurrentOs");
            disabledOnOsAnnotationValue = disabledOnOsAnnotation.getMethod("value");
            registerExtensionAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass(RegisterExtension.class.getName());
            quarkusTestAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass("io.quarkus.test.junit.QuarkusTest");
            quarkusIntegrationTestAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass(QuarkusIntegrationTest.class.getName());
            profileAnnotation = (Class<? extends Annotation>) annotationLoader
                    .loadClass(TestProfile.class.getName());
            testAnnotation = (Class<? extends Annotation>) annotationLoader.loadClass(Test.class.getName());
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // If QuarkusTest is not on the classpath, that's fine; it just means we definitely won't have QuarkusTests. That means we can bypass a whole bunch of logic.
            log.debug("Could not load annotations for FacadeClassLoader: " + e);
        }
        // We want to see what services are registered, but without going through the service loader, since that results in a huge catastrophe of class not found exceptions
        // as the servoce loader tries to instantiate things in a nobbled loader. Instead, do it in a crude, safe, way by looking for the resource files and reading them.
        try {
            Enumeration<URL> declaredExtensions = annotationLoader
                    .getResources("META-INF/services/org.junit.jupiter.api.extension.Extension");
            while (declaredExtensions.hasMoreElements()) {
                URL url = declaredExtensions.nextElement();
                try (InputStream in = url.openStream()) {
                    String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (QuarkusTestExtension.class.getName()
                            .equals(contents)) {
                        isServiceLoaderMechanism = true;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Could not check service loader registrations: " + e);
            throw new RuntimeException(e);
        }

        if (profileNames != null) {
            this.profiles = new HashMap<>();

            profileNames.forEach((k, profileName) -> {
                Class<?> profile;
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
        log.debugf("Facade classloader loading %s", name);

        if (peekingClassLoader == null) {
            throw new RuntimeException("Attempted to load classes with a closed classloader: " + this);
        }
        boolean isQuarkusTest = false;
        boolean isIntegrationTest = false;
        Class<?> inspectionClass = null;

        // If the service loader mechanism is being used, QuarkusTestExtension gets loaded before any extensions which use it. We need to make sure it's on the right classloader.
        if (isServiceLoaderMechanism && (name.equals(QuarkusTestExtension.class.getName()))) {
            try {
                // We don't have enough information to make a runtime classloader yet, but we can make a curated application and a base classloader
                QuarkusClassLoader runtimeClassLoader = getOrCreateBaseClassLoader(getProfileKey(null), null);
                return runtimeClassLoader.loadClass(name);
            } catch (AppModelResolverException | BootstrapException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {

            Class<?> profile = null;
            if (profiles != null && !isServiceLoaderMechanism) {
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

                    if (!inspectionClass.isAnnotation()) {

                        if (isServiceLoaderMechanism) {
                            List<Method> anns = AnnotationSupport.findAnnotatedMethods(inspectionClass, testAnnotation,
                                    HierarchyTraversalMode.BOTTOM_UP);
                            // If a service loader was used to register QuarkusTestExtension, every JUnit test is a QuarkusTest
                            isQuarkusTest = !anns.isEmpty();
                        }

                        // Because (until we do https://github.com/quarkusio/quarkus/issues/45785) we start dev services for disabled tests when we load them with the quarkus classloader, and those dev services often fail, put in some
                        // bypasses.
                        // These bypasses are also useful for performance.
                        // Ideally we would check for every way of disabling a test, but we don't want to recreate the JUnit logic, so just do common ones that might be guarding classes with classloading or dev-service-starting issues
                        // That does mean our behaviour in this area is slightly inconsistent between annotations; for some we will augment before giving up and for some we won't
                        boolean isDisabled = AnnotationSupport.isAnnotated(inspectionClass, disabledAnnotation)
                                || isDisabledOnOs(inspectionClass);

                        // If a whole test class has an @Disabled annotation, do not bother creating a quarkus app for it
                        // Pragmatically, this fixes a LinkageError in grpc-cli which only reproduces in CI, but it's also probably what users would expect
                        if (!isDisabled) {
                            // There are several ways a test could be identified as a QuarkusTest:
                            // A Quarkus Test could be annotated with @QuarkusTest or with @ExtendWith[... QuarkusTestExtension.class ] or @RegisterExtension (or the service loader mechanism could be used)
                            // An @interface isn't a quarkus test, and doesn't want its own application; to detect it, just check if it has a superclass

                            isQuarkusTest = isQuarkusTest
                                    || AnnotationSupport.isAnnotated(inspectionClass,
                                            quarkusTestAnnotation) // AnnotationSupport picks up cases where a class is annotated with an annotation which itself includes the annotation we care about
                                    || registersQuarkusTestExtensionWithExtendsWith(inspectionClass)
                                    || registersQuarkusTestExtensionOnField(inspectionClass);

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
            }

            if (isQuarkusTest && !isIntegrationTest) {

                preloadTestResourceClasses(inspectionClass);
                QuarkusClassLoader runtimeClassLoader = getQuarkusClassLoader(inspectionClass, profile);
                Class<?> clazz = runtimeClassLoader.loadClass(name);

                return clazz;
            } else {
                return super.loadClass(name);
            }

        } catch (NoSuchMethodException e) {
            // TODO better handling of these
            System.err.println("Could not get method " + e);
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            System.err.println("Could not invoke " + e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            System.err.println("Could not access " + e);
            throw new RuntimeException(e);
        }

    }

    private boolean isDisabledOnOs(Class<?> inspectionClass) {
        Optional<? extends Annotation> ma = AnnotationSupport.findAnnotation(inspectionClass, disabledOnOsAnnotation);
        if (ma.isPresent()) {
            Annotation a = ma.get();
            try {
                Object values = disabledOnOsAnnotationValue.invoke(a);
                if (values.getClass().isArray()) {
                    int length = Array.getLength(values);
                    for (int i = 0; i < length; i++) {
                        Object value = Array.get(values, i); // an OS, but we can't cast to it because it's in a different classloader
                        boolean matches = (boolean) osIsCurrent.invoke(value);
                        if (matches) {
                            return true;
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private boolean registersQuarkusTestExtensionWithExtendsWith(Class<?> inspectionClass) {

        Optional<? extends Annotation> a = AnnotationSupport.findAnnotation(inspectionClass, extendWithAnnotation);
        // This toString looks sloppy, but we cannot do an equals() because there might be multiple JUnit extensions, and getting the annotation value would need a reflective call, which may not be any cheaper or prettier than doing the string
        return (a.isPresent() && a.get().toString().contains(QuarkusTestExtension.class.getName()));

    }

    /*
     * What's this for?
     * It's a bit like detecting the location in a privacy test or detecting the lab environment in an emissions test and then
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
                Class<?> resourceClass = (Class<?>) m.invoke(a);
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

        try {
            // We are looking for an instance of QuarkusTestExtension with a @RegistersExtension annotation
            List<Field> fields = AnnotationSupport.findAnnotatedFields(inspectionClass, registerExtensionAnnotation,
                    f -> f.getType()
                            .getName()
                            .equals(QuarkusTestExtension.class.getName()));

            return fields != null && !fields.isEmpty();
        } catch (NoClassDefFoundError e) {
            // Under the covers, JUnit calls getDefinedFields, and that sometimes throws class-related in native mode
            // With -Dnative loading the KeycloakRealmResourceManager gives a class not found exception for junit's TestRule
            // java.lang.RuntimeException: java.lang.NoClassDefFoundError: org/junit/rules/TestRule
            // TODO it would be nice to diagnose why that's happening
            log.warn("Could not discover field annotations: " + e);
            return false;
        }

    }

    private QuarkusClassLoader getQuarkusClassLoader(Class<?> requiredTestClass, Class<?> profile) {
        String profileKey = getProfileKey(profile);

        try {
            String key;
            QuarkusClassLoader classLoader;

            // We cannot directly access TestResourceUtil as long as we're in the core module, but the app classloaders can.
            // But, chicken-and-egg, we may not have an app classloader yet. However, if we don't, we won't need to worry about restarts, but this instance clearly cannot need a restart

            // TODO do the experiment - can it now work to use the peekingclassloader instead of the keymaker?

            // If we make a classloader with a null profile, we get the problem of starting dev services multiple times, which is very bad (if temporary) - once that issue is fixed, could reconsider
            if (keyMakerClassLoader == null) {
                // Making a classloader uses the profile key to look up a curated application
                classLoader = getOrCreateRuntimeClassLoader(profileKey, requiredTestClass, profile);
                keyMakerClassLoader = classLoader;

                // We cannot use the startup action one because it's a base runtime classloader and so will not have the right access to application classes (they're in its banned list)
                final String resourceKey = requiredTestClass != null ? getResourceKey(requiredTestClass, profile) : null;

                // The resource key might be null, and that's ok
                key = profileKey + resourceKey;
            } else {
                final String resourceKey = requiredTestClass != null ? getResourceKey(requiredTestClass, profile) : null;

                // The resource key might be null, and that's ok
                key = profileKey + resourceKey;
                classLoader = runtimeClassLoaders.get(key);
                if (classLoader == null) {
                    // Making a classloader uses the profile key to look up a curated application
                    classLoader = getOrCreateRuntimeClassLoader(profileKey, requiredTestClass, profile);
                }
            }

            runtimeClassLoaders.put(key, classLoader);
            return classLoader;
        } catch (RuntimeException e) {
            // Exceptions here get swallowed by the JUnit framework and we don't get any debug information unless we print it ourself
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            // Exceptions here get swallowed by the JUnit framework and we don't get any debug information unless we print it ourself
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String getProfileKey(Class<?> profile) {
        final String profileName = profile != null ? profile.getName() : NO_PROFILE;
        String profileKey = KEY_PREFIX + profileName;
        return profileKey;
    }

    private String getResourceKey(Class<?> requiredTestClass, Class<?> profile)
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

    private CuratedApplication getOrCreateCuratedApplication(String key, Class<?> requiredTestClass)
            throws IOException, AppModelResolverException, BootstrapException {
        CuratedApplication curatedApplication = curatedApplications.get(key);

        if (curatedApplication == null) {
            String displayName = DISPLAY_NAME_PREFIX + key;
            // TODO should we use clonedBuilder here, like TestSupport does?
            curatedApplication = AppMakerHelper.makeCuratedApplication(requiredTestClass, displayName,
                    isAuxiliaryApplication);
            curatedApplications.put(key, curatedApplication);

        }

        return curatedApplication;
    }

    private QuarkusClassLoader getOrCreateBaseClassLoader(String key, Class<?> requiredTestClass)
            throws AppModelResolverException, BootstrapException, IOException {
        CuratedApplication curatedApplication = getOrCreateCuratedApplication(key, requiredTestClass);
        return curatedApplication.getOrCreateBaseRuntimeClassLoader();
    }

    private QuarkusClassLoader getOrCreateRuntimeClassLoader(String key, Class<?> requiredTestClass, Class<?> profile)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException, AppModelResolverException, BootstrapException, IOException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        CuratedApplication curatedApplication = getOrCreateCuratedApplication(key, requiredTestClass);

        // Before interacting with the profiles, set the TCCL to one which is not the FacadeClassloader
        // This could also go in AppMakerHelper.setExtraProperties, but then it affects more code paths
        StartupAction startupAction;
        try {
            if (profile != null) {
                Thread.currentThread().setContextClassLoader(profile.getClassLoader());
            }
            startupAction = AppMakerHelper.getStartupAction(requiredTestClass,
                    curatedApplication, profile);

        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        // If the try block fails, this would be null, but there's no catch, so we'd never get to this code
        QuarkusClassLoader loader = startupAction.getClassLoader();

        Class<?> configProviderResolverClass = loader.loadClass(ConfigProviderResolver.class.getName());

        Class<?> testConfigProviderResolverClass = loader.loadClass(QuarkusTestConfigProviderResolver.class.getName());
        Object testConfigProviderResolver = testConfigProviderResolverClass.getDeclaredConstructor()
                .newInstance();

        configProviderResolverClass.getDeclaredMethod("setInstance", configProviderResolverClass)
                .invoke(null, testConfigProviderResolver);

        return loader;

    }

    public boolean isServiceLoaderMechanism() {
        return isServiceLoaderMechanism;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void close() throws IOException {

        if (peekingClassLoader != null) {
            peekingClassLoader.close();
            peekingClassLoader = null;
        }

        // Null out the keymaker classloader and runtime classloaders, but don't close them, since we assume they will be closed by the test framework closing the owning application
        keyMakerClassLoader = null;
        runtimeClassLoaders.clear();

    }

}
