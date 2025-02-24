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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.support.AnnotationSupport;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.dev.testing.AppMakerHelper;

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
     * // TODO should we use the canary loader, or the parent loader?
     * // TODO we need to close this when we're done
     * //If we use the parent loader, does that stop the quarkus classloaders getting a crack at some classes?
     */
    private final URLClassLoader peekingClassLoader;
    private Map<String, Class<?>> profiles;
    private String classesPath;
    private Set<String> quarkusTestClasses;
    private boolean isAuxiliaryApplication;
    private QuarkusClassLoader keyMakerClassLoader;

    private static volatile FacadeClassLoader instance;

    public static void clearSingleton() {
        if (instance != null) {
            instance.close();
        }
        instance = null;
    }

    // TODO does it make sense to have a parent here when it is sometimes ignored?
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
            instance = new FacadeClassLoader(parent, isAuxiliaryApplication, profiles, quarkusTestClasses, classesPath);
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
            String... classPaths) {
        super(parent);
        this.quarkusTestClasses = quarkusTestClasses;
        this.isAuxiliaryApplication = isAuxiliaryApplication;

        this.classesPath = String.join(File.pathSeparator, classPaths);
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
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);
        peekingClassLoader = new ParentLastURLClassLoader(urls, parent);

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
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        System.out.println("HOLLY facade classloader loading " + name);
        boolean isQuarkusTest = false;

        boolean isIntegrationTest = false;
        // TODO hack that didn't even work
        //        if (runtimeClassLoader != null && name.contains("QuarkusTestProfileAwareClass")) {
        //            return runtimeClassLoader.loadClass(name);
        //        } else if (name.contains("QuarkusTestProfileAwareClass")) {
        //            return this.getClass().getClassLoader().loadClass(name);
        //
        //        }
        // TODO we can almost get away with using a string, except for type safety - maybe a dotname?
        // TODO since of course avoiding the classload would be ideal
        // Lots of downstream logic uses the class to work back to the classpath, so we can't just get rid of it (yet)
        // ... but of course at this stage we don't know anything more about the classpath than anyone else, and are just using the system property
        // ... so anything using this to get the right information will be disappointed
        // TODO we should just pass through the moduleInfo, right?
        Class<?> fromCanary = null;

        try {
            if (peekingClassLoader != null) {
                try {
                    fromCanary = peekingClassLoader
                            .loadClass(name);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    return super.loadClass(name);
                }
            }

            // TODO  should we use JUnit's AnnotationSupport? It searches class hierarchies. Unless we have a good reason not to use it, perhaps we should?
            // See, for example, https://github.com/marcphilipp/gradle-sandbox/blob/baaa1972e939f5817f54a3d287611cef0601a58d/classloader-per-test-class/src/test/java/org/example/ClassLoaderReplacingLauncherSessionListener.java#L23-L44
            // One reason not to use it is that it needs an annotation class, but we can load one with the canary
            // It looks up the hierarchy which our current logic doesn't, which is risky

            Class<?> profile = null;
            if (profiles != null) {
                // TODO the good is that we're re-using what JUnitRunner already worked out, the bad is that this is seriously clunky with multiple code paths, brittle information sharing ...
                // TODO at the very least, should we have a test landscape holder class?
                // The JUnitRunner counts main tests as quarkus tests
                isQuarkusTest = quarkusTestClasses.contains(name);

                profile = profiles.get(name);

            } else {
                // TODO JUnitRunner already worked all this out for the dev mode case, could we share some logic?

                // TODO make this test cleaner + more rigorous
                // A Quarkus Test could be annotated with @QuarkusTest or with @ExtendWith[... QuarkusTestExtension.class ] or @RegisterExtension
                // An @interface isn't a quarkus test, and doesn't want its own application; to detect it, just check if it has a superclass - except that fails for things whose superclass isn't on the classpath, like javax.tools subclasses
                // TODO we probably need to walk the class hierarchy for the annotations, too? or do they get added to getAnnotations?
                isQuarkusTest = !fromCanary.isAnnotation() && Arrays.stream(fromCanary.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType()
                                .getName()
                                .endsWith("QuarkusTest"))
                        || Arrays.stream(fromCanary.getAnnotations())
                                .anyMatch(annotation -> annotation.annotationType()
                                        .getName()
                                        .endsWith("org.junit.jupiter.api.extension.ExtendWith")
                                        && annotation.toString()
                                                .contains(
                                                        "io.quarkus.test.junit.QuarkusTestExtension")) // TODO should this be an equals(), for performance? Probably can do a better check than toString, which adds an @ and a .class
                        // (I think)
                        || registersQuarkusTestExtension(fromCanary);

                // TODO want to exclude quarkus component test, and exclude quarkusmaintest - what about quarkusunittest? and quarkusintegrationtest?
                // TODO knowledge of test annotations leaking in to here, although JUnitTestRunner also has the same leak - should we have a superclass that lives in this package that we check for?
                // TODO be tighter with the names we check for
                // TODO this would be way easier if this was in the same module as the profile, could just do clazz.getAnnotation(TestProfile.class)

                isIntegrationTest = Arrays.stream(fromCanary.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType()
                                .getName()
                                .endsWith("QuarkusIntegrationTest"));

                Optional<Annotation> profileAnnotation = Arrays.stream(fromCanary.getAnnotations())
                        .filter(annotation -> annotation.annotationType()
                                .getName()
                                .endsWith("TestProfile"))
                        .findFirst();
                if (profileAnnotation.isPresent()) {

                    // TODO could do getAnnotationsByType if we were in the same module
                    Method m = profileAnnotation.get()
                            .getClass()
                            .getMethod("value");
                    // We can't be specific about what the class extends, because it's loaded with another classloader
                    profile = (Class<?>) m.invoke(profileAnnotation.get());
                }
            }

            // TODO move this into the getclassloade method
            // TODO would we ever load if it's not a quarkus test?
            final String profileName = profile != null ? profile.getName() : NO_PROFILE;
            String profileKey = isQuarkusTest ? "QuarkusTest" + "-" + profileName
                    : "vanilla";
            // TODO do we need to do extra work to make sure all of the quarkus app is in the cp? We'll return versions from the parent otherwise
            // TODO think we need to make a 'first' runtime cl, and then switch for each new test?
            // TODO how do we decide what to load with our classloader - everything?
            // Doing it just for the test loads too little, doing it for everything gives java.lang.ClassCircularityError: io/quarkus/runtime/configuration/QuarkusConfigFactory
            // Anything loaded by JUnit will come through this classloader

            if (isQuarkusTest && !isIntegrationTest) {

                preloadTestResourceClasses(fromCanary);
                QuarkusClassLoader runtimeClassLoader = getQuarkusClassLoader(profileKey, fromCanary, profile);
                System.out.println("HOLLY made classloader " + runtimeClassLoader);
                Class thing = runtimeClassLoader.loadClass(name);
                System.out.println("HOLLY did load " + thing + " using CL " + thing.getClassLoader());

                return thing;
            } else {
                return super.loadClass(name);
            }

        } catch (NoSuchMethodException e) {
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
                        .getMethod("value");
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

    private boolean registersQuarkusTestExtension(Class<?> fromCanary) {
        Class<?> clazz = fromCanary;
        try {
            while (clazz != null) {
                // TODO this call is not safe in our nobbled classloader, which is sort of surprising since I thought declared meant 'on this class' but I guess it needs to be able to access the parent
                for (Field field : clazz.getDeclaredFields()) {
                    // We can't use isAnnotationPresent because the classloader of the JUnit classes will be wrong (the canary classloader rather than our classloader)
                    // TODO will all this searching be dreadfully slow?
                    // TODO redo the canary loader to load JUnit classes with the same classloader as this?

                    if (Arrays.stream(field.getAnnotations())
                            .anyMatch(annotation -> annotation.annotationType()
                                    .getName()
                                    .equals(RegisterExtension.class.getName()))) {
                        if (field.getType()
                                .getName()
                                .equals("io.quarkus.test.junit.QuarkusTestExtension")) {
                            return true;
                        }
                    }
                }

                clazz = clazz.getSuperclass();
            }
        } catch (NoClassDefFoundError e) {
            // Because the canary loader doesn't have a parent, this is possible
            // We also see this error in getDeclaredFields(), which is more surprising to me
            // If it happens, assume it's ok
            // It's very unlikely something on the app classloader will extend quarkus test
            // TODO suppress error once we know this is safe
            System.out.println("HOLLY could not get parent of " + clazz.getName() + " got error " + e);
        }

        return false;
    }

    private QuarkusClassLoader getQuarkusClassLoader(String profileKey, Class requiredTestClass, Class<?> profile) {
        try {

            StartupAction startupAction;
            String key = null;

            // We cannot directly access TestResourceUtil as long as we're in the core module, but the app classloaders can.
            // But, chicken-and-egg, we may not have an app classloader yet. However, if we don't, we won't need to worry about restarts, but this instance clearly cannot need a restart

            // If we make a classloader with a null profile, we get the problem of starting dev services multiple times, which is very bad (if temporary) - once that TODO issue is fixed, could reconsider
            if (keyMakerClassLoader == null) {
                // Making a classloader uses the profile key to look up a curated application
                // TODO this may need to be a dummy startup action with no profile,
                //so must not re-use it unless confirmed profile is null

                startupAction = makeClassLoader(profileKey, requiredTestClass, profile);
                keyMakerClassLoader = startupAction.getClassLoader();
                // TODO if we do keep it as the system classloader, all this can become much simpler
                // We cannot use the startup action one because it's a base runtime classloader and so will not have the right access to application classes (they're in its banned list)

                final String resourceKey = getResourceKey(requiredTestClass, profile);

                // The resource key might be null, and that's ok
                key = profileKey + resourceKey;
            } else {

                final String resourceKey = getResourceKey(requiredTestClass, profile);

                // The resource key might be null, and that's ok
                key = profileKey + resourceKey;
                // TODO only do this and the next bit if the keymaker classloader is not suitable
                startupAction = runtimeClassLoaders.get(key);
                System.out.println("HOLLY seen this key before " + startupAction);

                if (startupAction == null) {
                    // TODO can we make this less confusing?

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

    private String getResourceKey(Class requiredTestClass, Class profile)
            throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {

        String resourceKey;
        // TODO revise this comment Make sure to use a classloader for the testresourceutil which is compatible with the profile we are passing in
        // TODO we can probably just use it without reflectively invoking it? Ah, no, wrong module - which brings us back to wanting to get this into the JUnit5 module
        //TODO does it need to be the key maker, or can it be our classloader? That'd be a lot simpler ...
        // TODO cache this?
        ClassLoader classLoader = keyMakerClassLoader; // TODO profile == null ? keyMakerClassLoader : profile.getClassLoader();
        Method method = Class
                .forName("io.quarkus.test.junit.TestResourceUtil", true, classLoader) // TODO use class, not string, but that would need us to be in a different module
                .getMethod("getReloadGroupIdentifier", Class.class, Class.class);

        // TODO this is kind of annoying, can we find a nicer way?
        // The resource checks assume that there's a useful TCCL and load the class with that TCCL to do reference equality checks and casting against resource classes
        // That does mean we potentially load the test class three times, if there's resources
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

        // This interception is only actually needed in limited circumstances; when
        // - running in normal mode
        // - *and* there is a @QuarkusTest to run

        // This class sets a Thead Context Classloader, which JUnit uses to load classes.
        // However, in continuous testing mode, setting a TCCL here isn't sufficient for the
        // tests to come in with our desired classloader;
        // downstream code sets the classloader to the deployment classloader, so we then need
        // to come in *after* that code.

        // TODO sometimes this is called in dev mode and sometimes it isn't? Ah, it's only not
        //  called if we die early, before we get to this

        // In continuous testing mode, the runner code will have executed before this
        // interceptor, so
        // this interceptor doesn't need to do anything.
        // TODO what if we removed the changes in the runner code?

        //  TODO I think all these comments are wrong? Bypass all this in continuous testing mode, where the custom runner will have already initialised things before we hit this class; the startup action holder is our best way
        // of detecting it

        // TODO alternate way of detecting it ? Needs the build item, though
        // TODO could the extension pass this through to us? no, I think we're invoked before anything quarkusy, and junit5 isn't even an extension
        //        DevModeType devModeType = launchModeBuildItem.getDevModeType().orElse(null);
        //        if (devModeType == null || !devModeType.isContinuousTestingSupported()) {
        //            return;
        //        }

        // Some places do this, but that assumes we already have a classloader!         boolean isContinuousTesting = testClassClassLoader instanceof QuarkusClassLoader;

        Thread currentThread = Thread.currentThread();

        AppMakerHelper appMakerHelper = new AppMakerHelper();

        CuratedApplication curatedApplication = curatedApplications.get(key);

        if (curatedApplication == null) {
            Collection shutdownTasks = new HashSet();

            String displayName = "JUnit" + key; // TODO come up with a good display name
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

        System.out.println("HOLLY at end of classload TCCL is " + currentThread.getContextClassLoader());
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
