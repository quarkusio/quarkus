package io.quarkus.deployment.pkg.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

class GeneratedResourceBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClass(UberJarMain.class))
            .setApplicationName("generated-resource")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true)
            .overrideConfigKey("quarkus.package.jar.type", "uber-jar")
            .setForcedDependencies(List.of(
                    Dependency.of("org.apache.cxf", "cxf-rt-bindings-xml", "3.4.3"),
                    Dependency.of("org.apache.cxf", "cxf-rt-bindings-soap", "3.4.3")));

    @Test
    public void testXMLResourceWasMerged() {
        Assumptions.assumeTrue(isOnline());
        assertThat(runner.getStartupConsoleOutput()).contains("RESOURCES: 1",
                "<entry key=\"xml-javax.wsdl.Port\">org.apache.cxf.binding.xml.wsdl11.HttpAddressPlugin</entry>",
                "<entry key=\"xml-javax.wsdl.Binding\">org.apache.cxf.binding.xml.wsdl11.XmlBindingPlugin</entry>",
                "<entry key=\"xml-javax.wsdl.BindingInput\">org.apache.cxf.binding.xml.wsdl11.XmlIoPlugin</entry>",
                "<entry key=\"xml-javax.wsdl.BindingOutput\">org.apache.cxf.binding.xml.wsdl11.XmlIoPlugin</entry>",
                "<entry key=\"soap-javax.wsdl.Port\">org.apache.cxf.binding.soap.wsdl11.SoapAddressPlugin</entry>");
        assertThat(runner.getExitCode()).isZero();
    }

    @QuarkusMain
    public static class UberJarMain {

        public static void main(String[] args) throws IOException {
            List<URL> resources = Collections
                    .list(UberJarMain.class.getClassLoader().getResources("META-INF/wsdl.plugin.xml"));
            System.out.println("RESOURCES: " + resources.size());
            try (InputStream is = resources.get(0).openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                reader.lines().forEach(System.out::println);
            }
        }
    }

    boolean isOnline() {
        try {
            InetAddress resolved = InetAddress.getByName("sun.com");
            return resolved != null;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
