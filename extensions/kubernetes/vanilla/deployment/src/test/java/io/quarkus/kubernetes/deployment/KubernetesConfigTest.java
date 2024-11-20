package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.KubernetesConfigTest.Reader.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.yaml.snakeyaml.Yaml;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

class KubernetesConfigTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withConfigurationResource("application-kubernetes.properties")
            .setRun(true);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    private Map<String, Object> secret;
    private Map<String, Object> serviceAccount;
    private Map<String, Object> deployment;
    private Map<String, Object> service;
    private Map<String, Object> container;
    private Map<String, Object> role;
    private Map<String, Object> clusterRole;
    private Map<String, Object> ingress;

    @Test
    @SuppressWarnings("unchecked")
    void kubernetes() throws Exception {
        Path outputDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        Iterable<Object> objects = new Yaml().loadAll(new String(Files.readAllBytes(outputDir.resolve("kubernetes.yml"))));
        for (Object object : objects) {
            if (((Map<String, Object>) object).get("kind").equals("Secret")) {
                secret = (Map<String, Object>) object;
            } else if (((Map<String, Object>) object).get("kind").equals("ServiceAccount")) {
                serviceAccount = (Map<String, Object>) object;
            } else if (((Map<String, Object>) object).get("kind").equals("Service")) {
                service = (Map<String, Object>) object;
            } else if (((Map<String, Object>) object).get("kind").equals("Deployment")) {
                deployment = (Map<String, Object>) object;
            } else if (((Map<String, Object>) object).get("kind").equals("Role")) {
                role = (Map<String, Object>) object;
            } else if (((Map<String, Object>) object).get("kind").equals("ClusterRole")) {
                clusterRole = (Map<String, Object>) object;
            } else if (((Map<String, Object>) object).get("kind").equals("Ingress")) {
                ingress = (Map<String, Object>) object;
            }
        }

        container = deployment().map("spec").map("template").map("spec").map("containers").list(0).asMap();

        assertEquals("konoha", service().map("metadata").asMap("labels").get("app.kubernetes.io/part-of"));
        assertEquals("naruto", service().map("metadata").asMap("labels").get("app.kubernetes.io/name"));
        assertEquals("7th", service().map("metadata").asMap("labels").get("app.kubernetes.io/version"));
        assertEquals("minato", service().map("metadata").asMap("labels").get("father"));
        assertEquals("kushina", service().map("metadata").asMap("labels").get("mother"));
        assertEquals("kage", service().map("metadata").asMap("annotations").get("rank"));
        assertEquals("uzumaki", service().map("metadata").asMap("annotations").get("clan"));
        assertNotNull(service().map("metadata").asMap("annotations").get("app.quarkus.io/build-timestamp"));
        assertEquals("land-of-fire", service().asMap("metadata").get("namespace"));
        assertEquals("rikudo", read(serviceAccount).map("metadata").asMap().get("name"));

        assertEquals("hokage-office", container.get("workingDir"));
        assertEquals("rasengan", ((List<String>) container.get("command")).get(0));
        assertEquals("rasenshuriken", ((List<String>) container.get("command")).get(1));
        assertEquals("chakra", ((List<String>) container.get("args")).get(0));
        assertEquals("wind", ((List<String>) container.get("args")).get(1));

        List<Map<String, Object>> ports = service().map("spec").asList("ports");
        for (Map<String, Object> port : ports) {
            if (port.get("name").equals("http")) {
                assertEquals("http", port.get("name"));
                assertEquals("1234", port.get("targetPort").toString());
                assertEquals("4321", port.get("port").toString());
                assertEquals("30000", port.get("nodePort").toString());
                assertEquals("TCP", port.get("protocol"));
            } else if (port.get("name").equals("grpc")) {
                assertEquals("grpc", port.get("name"));
                assertEquals("2222", port.get("targetPort").toString());
                assertEquals("4444", port.get("port").toString());
                assertEquals("30001", port.get("nodePort").toString());
                assertEquals("TCP", port.get("protocol"));
            } else {
                fail();
            }
        }

        assertEquals("NodePort", service().asMap("spec").get("type"));
        assertEquals("666", deployment().asMap("spec").get("replicas").toString());
        assertEquals("RollingUpdate", deployment().map("spec").asMap("strategy").get("type"));
        assertEquals("99%", deployment().map("spec").map("strategy").asMap("rollingUpdate").get("maxSurge"));
        assertEquals("99%", deployment().map("spec").map("strategy").asMap("rollingUpdate").get("maxUnavailable"));

        assertEquals("IfNotPresent", container().asMap().get("imagePullPolicy"));

        Reader imagePullSecrets = deployment().map("spec").map("template").map("spec").map("imagePullSecrets");
        assertEquals("wind", imagePullSecrets.list(0).asMap().get("name"));
        assertEquals("fire", imagePullSecrets.list(1).asMap().get("name"));
        assertEquals("earth", imagePullSecrets.list(2).asMap().get("name"));
        assertNotNull(secret().asMap("data").get(".dockerconfigjson"));

        assertEquals("/action", ((List<String>) container().map("livenessProbe").asMap("exec").get("command")).get(0));
        assertEquals("localhost", container().map("livenessProbe").asMap("tcpSocket").get("host"));
        assertEquals("4231", container().map("livenessProbe").asMap("tcpSocket").get("port"));
        assertEquals("4444", container().map("livenessProbe").asMap("grpc").get("port").toString());
        assertEquals("666", container().asMap("livenessProbe").get("initialDelaySeconds").toString());
        assertEquals("1", container().asMap("livenessProbe").get("periodSeconds").toString());
        assertEquals("1", container().asMap("livenessProbe").get("timeoutSeconds").toString());
        assertEquals("1", container().asMap("livenessProbe").get("successThreshold").toString());
        assertEquals("0", container().asMap("livenessProbe").get("failureThreshold").toString());
        assertEquals("/action", ((List<String>) container().map("readinessProbe").asMap("exec").get("command")).get(0));
        assertEquals("localhost", container().map("readinessProbe").asMap("tcpSocket").get("host"));
        assertEquals("4231", container().map("readinessProbe").asMap("tcpSocket").get("port"));
        assertEquals("4444", container().map("readinessProbe").asMap("grpc").get("port").toString());
        assertEquals("666", container().asMap("readinessProbe").get("initialDelaySeconds").toString());
        assertEquals("1", container().asMap("readinessProbe").get("periodSeconds").toString());
        assertEquals("1", container().asMap("readinessProbe").get("timeoutSeconds").toString());
        assertEquals("1", container().asMap("readinessProbe").get("successThreshold").toString());
        assertEquals("0", container().asMap("readinessProbe").get("failureThreshold").toString());
        assertEquals("/action", ((List<String>) container().map("startupProbe").asMap("exec").get("command")).get(0));
        assertEquals("localhost", container().map("startupProbe").asMap("tcpSocket").get("host"));
        assertEquals("4231", container().map("startupProbe").asMap("tcpSocket").get("port"));
        assertEquals("4444", container().map("startupProbe").asMap("grpc").get("port").toString());
        assertEquals("666", container().asMap("startupProbe").get("initialDelaySeconds").toString());
        assertEquals("1", container().asMap("startupProbe").get("periodSeconds").toString());
        assertEquals("1", container().asMap("startupProbe").get("timeoutSeconds").toString());
        assertEquals("1", container().asMap("startupProbe").get("successThreshold").toString());
        assertEquals("0", container().asMap("startupProbe").get("failureThreshold").toString());

        List<Map<String, Object>> volumeMounts = container().asList("volumeMounts");
        for (Map<String, Object> volumeMount : volumeMounts) {
            if (volumeMount.get("name").equals("fire")) {
                assertEquals("fire", volumeMount.get("name"));
                assertEquals("fire", volumeMount.get("mountPath"));
                assertEquals("fire", volumeMount.get("subPath"));
                assertEquals("true", volumeMount.get("readOnly").toString());
            } else if (volumeMount.get("name").equals("water")) {
                assertEquals("water", volumeMount.get("name"));
                assertEquals("water", volumeMount.get("mountPath"));
                assertEquals("water", volumeMount.get("subPath"));
                assertEquals("true", volumeMount.get("readOnly").toString());
            } else if (volumeMount.get("name").equals("app-secret")) {
                assertEquals("/mnt/app-secret", volumeMount.get("mountPath"));
            } else if (volumeMount.get("name").equals("app-config-map")) {
                assertEquals("/mnt/app-config-map", volumeMount.get("mountPath"));
            } else {
                fail();
            }
        }

        List<Map<String, Object>> volumes = deployment().map("spec").map("template").map("spec").asList("volumes");
        for (Map<String, Object> volume : volumes) {
            if (volume.get("name").equals("wind")) {
                assertEquals("wind", read(volume).asMap("secret").get("secretName"));
                assertEquals("511", read(volume).asMap("secret").get("defaultMode").toString());
                assertEquals("true", read(volume).asMap("secret").get("optional").toString());
                Map<String, Object> items = read(read(volume).asMap("secret").get("items")).list(0).asMap();
                assertEquals("wind", items.get("key"));
                assertEquals("wind", items.get("path"));
                assertEquals("777", items.get("mode").toString());
            } else if (volume.get("name").equals("earth")) {
                assertEquals("earth", read(volume).asMap("configMap").get("name"));
                assertEquals("511", read(volume).asMap("configMap").get("defaultMode").toString());
                assertEquals("true", read(volume).asMap("configMap").get("optional").toString());
                Map<String, Object> items = read(read(volume).asMap("configMap").get("items")).list(0).asMap();
                assertEquals("earth", items.get("key"));
                assertEquals("earth", items.get("path"));
                assertEquals("777", items.get("mode").toString());
            } else if (volume.get("name").equals("one")) {
                assertNotNull(read(volume).asMap("emptyDir"));
            } else if (volume.get("name").equals("two")) {
                assertNotNull(read(volume).asMap("emptyDir"));
            } else if (volume.get("name").equals("three")) {
                assertNotNull(read(volume).asMap("emptyDir"));
            } else if (volume.get("name").equals("gaara")) {
                assertEquals("gaara", read(volume).asMap("persistentVolumeClaim").get("claimName"));
                assertEquals("true", read(volume).asMap("persistentVolumeClaim").get("readOnly").toString());
            } else if (volume.get("name").equals("sakura")) {
                assertEquals("sakura", read(volume).asMap("awsElasticBlockStore").get("volumeID"));
                assertEquals("0", read(volume).asMap("awsElasticBlockStore").get("partition").toString());
                assertEquals("sakura", read(volume).asMap("awsElasticBlockStore").get("fsType"));
                assertEquals("true", read(volume).asMap("awsElasticBlockStore").get("readOnly").toString());
            } else if (volume.get("name").equals("shikamaru")) {
                assertEquals("shikamaru", read(volume).asMap("azureFile").get("shareName"));
                assertEquals("shikamaru", read(volume).asMap("azureFile").get("secretName").toString());
                assertEquals("true", read(volume).asMap("azureFile").get("readOnly").toString());
            } else if (volume.get("name").equals("temari")) {
                assertEquals("temari", read(volume).asMap("azureDisk").get("diskName"));
                assertEquals("temari", read(volume).asMap("azureDisk").get("diskURI"));
                assertEquals("Shared", read(volume).asMap("azureDisk").get("kind"));
                assertEquals("None", read(volume).asMap("azureDisk").get("cachingMode"));
                assertEquals("temari", read(volume).asMap("azureDisk").get("fsType"));
                assertEquals("true", read(volume).asMap("azureDisk").get("readOnly").toString());
            } else if (volume.get("name").equals("app-config-map")) {
                assertEquals("sasuke", read(volume).asMap("configMap").get("name"));
            } else if (volume.get("name").equals("app-secret")) {
                assertEquals("sasuke", read(volume).asMap("secret").get("secretName"));
            } else {
                fail();
            }
        }

        int assertEnvCount = 0;
        for (Map<String, Object> env : (List<Map<String, Object>>) container.get("env")) {
            if (env.get("name").equals("UZUMAKI_NARUTO")) {
                assertEquals("naruto", env.get("value"));
                assertEnvCount++;
            }
        }
        assertTrue(assertEnvCount > 0);

        Map<String, Object> hostAliases = deployment().map("spec").map("template").map("spec").map("hostAliases").list(0)
                .asMap();
        assertEquals("konoha", hostAliases.get("ip"));
        assertIterableEquals(List.of("dev", "qly", "prod"), (Iterable<String>) hostAliases.get("hostnames"));

        Map<String, Object> nodeSelector = deployment().map("spec").map("template").map("spec").asMap("nodeSelector");
        assertTrue(nodeSelector.containsKey("jutsu"));
        assertEquals("katon", nodeSelector.get("jutsu"));

        Map<String, Object> limits = container().map("resources").asMap("limits");
        assertEquals("fuuton", limits.get("cpu"));
        assertEquals("raiton", limits.get("memory"));
        Map<String, Object> requests = container().map("resources").asMap("requests");
        assertEquals("katon", requests.get("cpu"));
        assertEquals("suiton", requests.get("memory"));

        assertEquals("kankuro", role().asMap("metadata").get("name"));
        assertEquals("kankuro", role().asMap("metadata").get("namespace"));
        assertEquals("sand", role().map("metadata").map("labels").asMap().get("sand"));
        assertNotNull(role().map("rules").list(0).asMap().get("apiGroups"));
        assertNotNull(role().map("rules").list(0).asMap().get("nonResourceURLs"));
        assertNotNull(role().map("rules").list(0).asMap().get("resourceNames"));
        assertNotNull(role().map("rules").list(0).asMap().get("resources"));
        assertNotNull(role().map("rules").list(0).asMap().get("verbs"));
        assertEquals("kakashi", clusterRole().asMap("metadata").get("name"));
        assertEquals("fire", clusterRole().map("metadata").map("labels").asMap().get("fire"));
        assertNotNull(clusterRole().map("rules").list(0).asMap().get("apiGroups"));
        assertNotNull(clusterRole().map("rules").list(0).asMap().get("nonResourceURLs"));
        assertNotNull(clusterRole().map("rules").list(0).asMap().get("resourceNames"));
        assertNotNull(clusterRole().map("rules").list(0).asMap().get("resources"));
        assertNotNull(clusterRole().map("rules").list(0).asMap().get("verbs"));

        assertEquals("tenten", ingress().map("metadata").map("annotations").asMap().get("tenten"));
        assertEquals("tenten", ingress().asMap("spec").get("ingressClassName"));
        assertEquals("tenten", ingress().map("spec").map("rules").list(0).asMap().get("host"));
        assertEquals("/http", ingress().map("spec").map("rules").list(0).map("http").map("paths").list(0).asMap().get("path"));
        assertEquals("Prefix",
                ingress().map("spec").map("rules").list(0).map("http").map("paths").list(0).asMap().get("pathType"));
    }

    @Test
    void openshift() throws Exception {
        Path outputDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        String openshift = new String(Files.readAllBytes(outputDir.resolve("openshift.yml")));
        assertNotNull(openshift);
    }

    private Reader secret() {
        return new Reader(secret);
    }

    private Reader service() {
        return new Reader(service);
    }

    private Reader deployment() {
        return new Reader(deployment);
    }

    private Reader container() {
        return new Reader(container);
    }

    private Reader role() {
        return new Reader(role);
    }

    private Reader clusterRole() {
        return new Reader(clusterRole);
    }

    private Reader ingress() {
        return new Reader(ingress);
    }

    @SuppressWarnings("unchecked")
    static class Reader {
        Object object;

        public Reader(final Object object) {
            this.object = object;
        }

        Reader map(String key) {
            return new Reader(((Map<String, Object>) object).get(key));
        }

        Reader list(int element) {
            return new Reader(((List<Map<String, Object>>) object).get(element));
        }

        Map<String, Object> asMap() {
            return (Map<String, Object>) object;
        }

        Map<String, Object> asMap(String key) {
            return (Map<String, Object>) ((Map<String, Object>) object).get(key);
        }

        List<Map<String, Object>> asList(String key) {
            return (List<Map<String, Object>>) ((Map<String, Object>) object).get(key);
        }

        static Reader read(final Object object) {
            return new Reader(object);
        }
    }
}
