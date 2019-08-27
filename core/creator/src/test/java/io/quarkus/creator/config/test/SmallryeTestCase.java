package io.quarkus.creator.config.test;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.MultirootedConfigHandler;
import io.quarkus.creator.config.reader.PropertiesConfigReader;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.config.reader.PropertyLine;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 *
 * @author Alexey Loubyansky
 */
public class SmallryeTestCase {

    @Test
    public void testMain() throws Exception {

        /*
         * User info
         */
        final PropertiesHandler<User> userHandler = new MappedPropertiesHandler<User>() {
            @Override
            public User getTarget() {
                return new User();
            }
        }
                .map("name", (User user, String value) -> user.setName(value))
                .map("country", (User user, String value) -> user.setCountry(value))
                .map("dir", (User user, String value) -> user.setDir(Paths.get(value)))
                .map("home", (User user, String value) -> user.setHome(Paths.get(value)))
                .map("language", (User user, String value) -> user.setLanguage(value));

        /*
         * java.vm.specification info
         */
        final PropertiesHandler<JavaVmSpec> javaVmSpecHandler = new MappedPropertiesHandler<JavaVmSpec>() {
            @Override
            public JavaVmSpec getTarget() {
                return new JavaVmSpec();
            }
        }
                .map("name", (JavaVmSpec vm, String value) -> vm.setName(value))
                .map("version", (JavaVmSpec vm, String value) -> vm.setVersion(value))
                .map("vendor", (JavaVmSpec vm, String value) -> vm.setVendor(value));

        /*
         * java.vm info
         */
        final PropertiesHandler<JavaVm> javaVmHandler = new MappedPropertiesHandler<JavaVm>() {
            @Override
            public JavaVm getTarget() {
                return new JavaVm();
            }
        }
                .map("name", (JavaVm vm, String value) -> vm.setName(value))
                .map("info", (JavaVm vm, String value) -> vm.setInfo(value))
                .map("version", (JavaVm vm, String value) -> vm.setVersion(value))
                .map("specification", javaVmSpecHandler, (JavaVm vm, JavaVmSpec spec) -> vm.setSpec(spec));

        /*
         * java info
         */
        final PropertiesHandler<JavaInfo> javaHandler = new MappedPropertiesHandler<JavaInfo>() {
            @Override
            public JavaInfo getTarget() {
                return new JavaInfo();
            }
        }
                .map("version", (JavaInfo java, String value) -> java.setVersion(value))
                .map("vm", javaVmHandler, (JavaInfo java, JavaVm vm) -> java.setVM(vm));

        /*
         * Main properties handler
         */
        final User[] user = new User[1];
        final JavaInfo[] javaInfo = new JavaInfo[1];
        final JavaVm[] vm = new JavaVm[1];
        final MultirootedConfigHandler configHandler = new MultirootedConfigHandler();
        configHandler.map("user", userHandler, (User parsed) -> user[0] = parsed);
        //configHandler.map("java", javaHandler, (JavaInfo parsed) -> javaInfo[0] = parsed);
        configHandler.map("java.vm", javaVmHandler, (JavaVm parsed) -> vm[0] = parsed);

        /*
         * Get the config
         */
        final Config config = SmallRyeConfigProviderResolver.instance().getConfig();

        /*
         * Read the config
         */
        PropertiesConfigReader.getInstance(
                configHandler, // properties handler
                (PropertyLine line) -> {
                } // ignore unrecognized properties, otherwise it's going to complain
        )
                // read config as properties
                .read(config.getPropertyNames(), (String name) -> {
                    return new PropertyLine(name, config.getOptionalValue(name, String.class).orElse(""));
                });

        final User expectedUser = new User();
        expectedUser.setCountry(getProperty("user.country"));
        expectedUser.setDir(Paths.get(getProperty("user.dir")));
        expectedUser.setHome(Paths.get(getProperty("user.home")));
        expectedUser.setLanguage(getProperty("user.language"));
        expectedUser.setName(getProperty("user.name"));
        assertEquals(expectedUser, user[0]);

        final JavaVmSpec expectedJvmSpec = new JavaVmSpec();
        expectedJvmSpec.setName(getProperty("java.vm.specification.name"));
        expectedJvmSpec.setVendor(getProperty("java.vm.specification.vendor"));
        expectedJvmSpec.setVersion(getProperty("java.vm.specification.version"));

        final JavaVm expectedJavaVm = new JavaVm();
        expectedJavaVm.setInfo(getProperty("java.vm.info"));
        expectedJavaVm.setName(getProperty("java.vm.name"));
        expectedJavaVm.setVersion(getProperty("java.vm.version"));
        expectedJavaVm.setSpec(expectedJvmSpec);

        assertEquals(expectedJavaVm, vm[0]);
    }

    private static String getProperty(String name) {
        return System.getProperty(name);
    }

}
