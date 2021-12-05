package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;

/**
 * Base util class for RESTEasy testing.
 */
public class TestUtil {

    protected static Logger LOG;

    private static String baseResourcePath = new StringBuilder()
            .append("src").append(File.separator)
            .append("test").append(File.separator)
            .append("resources").append(File.separator).toString();
    /**
     * Try to initialize logger. This is unsuccessful on EAP deployment, because EAP do not contain log4j.
     * Logger is not necessary for this class. Some methods could be used without it.
     */
    static {
        try {
            LOG = Logger.getLogger(TestUtil.class.getName());
        } catch (NoClassDefFoundError e) {
            // unable to initialize logger, finishContainerPrepare method could not be used
        }
    }

    public static WebArchive prepareArchiveWithApplication(String deploymentName, Class<? extends Application> clazz) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war");
        war.addClass(clazz);
        return war;
    }

    /**
     * Finish preparing war deployment and deploy it.
     *
     * Add classes in @resources to deployment. Also all sub-classes of classes in @resources are added to deployment.
     * But only classes in @resources (not sub-classes of classes in @resources) can be used as resources
     * (getClasses function of TestApplication class return only classes in @resources).
     *
     * @param resources classes used in deployment as resources
     */
    public static Archive<?> finishContainerPrepare(WebArchive war, Map<String, String> contextParams,
            final Class<?>... resources) {
        return finishContainerPrepare(war, contextParams, null, resources);
    }

    /**
     * Finish preparing war deployment and deploy it.
     *
     * Add classes in @resources to deployment. Also all sub-classes of classes in @resources are added to deployment.
     * But only classes in @resources (not sub-classes of classes in @resources) can be used as resources
     * (getClasses function of TestApplication class return only classes in @resources).
     *
     * @param singletons classes used in deployment as singletons
     * @param resources classes used in deployment as resources
     */
    public static Archive<?> finishContainerPrepare(WebArchive war, Map<String, String> contextParams,
            List<Class<?>> singletons, final Class<?>... resources) {

        if (contextParams == null) {
            contextParams = new Hashtable<>();
        }

        Set<String> classNamesInDeployment = new HashSet<>();
        Set<String> singletonsNamesInDeployment = new HashSet<>();

        if (resources != null) {
            for (final Class<?> clazz : resources) {
                war.addClass(clazz);
                classNamesInDeployment.add(clazz.getTypeName());
            }
        }

        if (singletons != null) {
            for (Class<?> singleton : singletons) {
                war.addClass(singleton);
                singletonsNamesInDeployment.add(singleton.getTypeName());
            }
        }

        if (contextParams != null && contextParams.size() > 0 && !war.contains("WEB-INF/web.xml")) {
            StringBuilder webXml = new StringBuilder();
            webXml.append("<web-app version=\"3.0\" xmlns=\"http://java.sun.com/xml/ns/javaee\" \n");
            webXml.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
            webXml.append(
                    " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"> \n");
            for (Map.Entry<String, String> entry : contextParams.entrySet()) {
                String paramName = entry.getKey();
                String paramValue = entry.getValue();
                LOG.debug("Context param " + paramName + " value " + paramValue);

                webXml.append("<context-param>\n");
                webXml.append("<param-name>" + paramName + "</param-name>\n");
                webXml.append("<param-value>" + paramValue + "</param-value>\n");
                webXml.append("</context-param>\n");
            }

            webXml.append("</web-app>\n");
            Asset resource = new StringAsset(webXml.toString());
            war.addAsWebInfResource(resource, "web.xml");
        }

        // prepare class list for getClasses function of TestApplication class
        StringBuilder classes = new StringBuilder();
        boolean start = true;
        for (String clazz : classNamesInDeployment) {
            if (start) {
                start = false;
            } else {
                classes.append(",");
            }
            classes.append(clazz);
        }
        war.addAsResource(new StringAsset(classes.toString()), "classes.txt");

        // prepare singleton list for getSingletons function of TestApplication class
        StringBuilder singletonBuilder = new StringBuilder();
        start = true;
        for (String clazz : singletonsNamesInDeployment) {
            if (start) {
                start = false;
            } else {
                singletonBuilder.append(",");
            }
            singletonBuilder.append(clazz);
        }
        war.addAsResource(new StringAsset(singletonBuilder.toString()), "singletons.txt");

        if (System.getProperty("STORE_WAR") != null) {
            war.as(ZipExporter.class).exportTo(new File("target", war.getName()), true);
        }
        return war;
    }

    /**
     * Add package info to deployment.
     *
     * @param clazz Package info is for package of this class.
     */
    protected WebArchive addPackageInfo(WebArchive war, final Class<?> clazz) {
        return war.addPackages(false, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath path) {
                return path.get().endsWith("package-info.class");
            }
        }, clazz.getPackage());
    }

    /**
     * Convert input stream to String.
     *
     * @param in Input stream
     * @return Converted string
     */
    public static String readString(final InputStream in) throws IOException {
        char[] buffer = new char[1024];
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        int wasRead = 0;
        do {
            wasRead = reader.read(buffer, 0, 1024);
            if (wasRead > 0) {
                builder.append(buffer, 0, wasRead);
            }
        } while (wasRead > -1);

        return builder.toString();
    }

    public static String getErrorMessageForKnownIssue(String jira, String message) {
        StringBuilder s = new StringBuilder();
        s.append("https://issues.jboss.org/browse/");
        s.append(jira);
        s.append(" - ");
        s.append(message);
        return s.toString();
    }

    public static String getErrorMessageForKnownIssue(String jira) {
        return getErrorMessageForKnownIssue(jira, "known issue");
    }

    public static String getJbossHome() {
        return System.getProperty("jboss.home");
    }

    public static String getJbossHome(boolean onServer) {
        if (onServer == false) {
            return getJbossHome();
        }
        return System.getProperty("jboss.home.dir", "");
    }

    /**
     * Get the path to the containers base dir for standalone mode (configuration, logs, etc..).
     * When arquillian.xml contains more containers that could be started simultaneously the parameter containerQualifier
     * is used to determine which base dir to get.
     * 
     * @param containerQualifier container qualifier or null if the arquillian.xml contains max 1 container available
     *        to be running at time
     * @return absolute path to base dir
     */
    public static String getStandaloneDir(String containerQualifier) {
        return getStandaloneDir(false, containerQualifier);
    }

    /**
     * Get the path to the containers base dir for standalone mode (configuration, logs, etc..).
     * When arquillian.xml contains more containers that could be started simultaneously the parameter containerQualifier
     * is used to determine which base dir to get.
     * 
     * @param onServer whether the check is made from client side (the path is constructed) or from deployment (the path
     *        is read from actual runtime value)
     * @param containerQualifier container qualifier or null if the arquillian.xml contains max 1 container available
     *        to be running at time; this has no effect when onServer is true
     * @return absolute path to base dir
     */
    public static String getStandaloneDir(boolean onServer, String containerQualifier) {
        if (onServer == false) {
            if (containerQualifier == null) {
                return new File(getJbossHome(), "standalone").getAbsolutePath();
            } else {
                return new File("target", containerQualifier).getAbsolutePath();
            }
        } else {
            return System.getProperty("jboss.server.base.dir", "");
        }
    }

    public static boolean isOpenJDK() {
        return System.getProperty("java.runtime.name").toLowerCase().contains("openjdk");
    }

    public static boolean isWildFly9x() {
        final String sv = System.getProperty("server.version");
        return ("9.0.2.Final".equals(sv) || "9.0.1.Final".equals(sv) || "9.0.0.Final".equals(sv));
    }

    public static boolean isOracleJDK() {
        if (isOpenJDK()) {
            return false;
        }
        String vendor = System.getProperty("java.vendor").toLowerCase();
        return vendor.contains("sun") || vendor.contains("oracle");
    }

    public static boolean isIbmJdk() {
        return System.getProperty("java.vendor").toLowerCase().contains("ibm");
    }

    /**
     * Get resource in test scope for some class.
     * Example: class org.test.MyTest and name "my_resource.txt" returns "src/test/resource/org/test/my_resource.txt"
     */
    public static String getResourcePath(Class<?> c, String name) {
        return new StringBuilder()
                .append(baseResourcePath)
                .append(c.getPackage().getName().replace('.', File.separatorChar))
                .append(File.separator).append(name)
                .toString();
    }

    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        if (osName == null) {
            Assertions.fail("Can't get the operating system name");
        }
        return (osName.indexOf("Windows") > -1) || (osName.indexOf("windows") > -1);
    }
}
