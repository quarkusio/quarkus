package io.quarkus.it.kafka.containers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.stream.Collectors;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class KerberosContainer extends GenericContainer<KerberosContainer> {

    public KerberosContainer(String dockerImageName) {
        super(dockerImageName);
        withStartupTimeout(Duration.ofMillis(20000));
        withEnv("KRB5_REALM", "EXAMPLE.COM");
        withEnv("KRB5_KDC", "localhost");
        withEnv("KRB5_PASS", "mypass");
        withExposedPorts(749, 464, 88);
        withFileSystemBind("src/test/resources/kafkabroker.keytab", "/tmp/keytab/kafkabroker.keytab", BindMode.READ_WRITE);
        withFileSystemBind("src/test/resources/client.keytab", "/tmp/keytab/client.keytab", BindMode.READ_WRITE);
        waitingFor(Wait.forLogMessage("Principal \"admin/admin@EXAMPLE.COM\" created.*", 1));
        withNetwork(Network.SHARED);
        withNetworkAliases("kerberos");
    }

    public void createTestPrincipals() {
        try {
            ExecResult lsResult = execInContainer("kadmin.local", "-q", "addprinc -randkey kafka/localhost@EXAMPLE.COM");
            lsResult = execInContainer("kadmin.local", "-q",
                    "ktadd -norandkey -k /tmp/keytab/kafkabroker.keytab kafka/localhost@EXAMPLE.COM");
            lsResult = execInContainer("kadmin.local", "-q", "addprinc -randkey client/localhost@EXAMPLE.COM");
            lsResult = execInContainer("kadmin.local", "-q",
                    "ktadd -norandkey -k /tmp/keytab/client.keytab client/localhost@EXAMPLE.COM");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createKrb5File() {
        try (FileInputStream fis = new FileInputStream("src/test/resources/krb5ClientTemplate.conf");
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                FileOutputStream file = new FileOutputStream("target/krb5.conf")) {
            String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            content = content.replaceAll("<host>", getHost());
            content = content.replaceAll("<kdc_port>", getMappedPort(88).toString());
            content = content.replaceAll("<admin_server_port>", getMappedPort(749).toString());
            file.write(content.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
