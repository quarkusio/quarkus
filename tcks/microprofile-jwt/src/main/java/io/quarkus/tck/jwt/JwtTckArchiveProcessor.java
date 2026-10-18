package io.quarkus.tck.jwt;

import java.io.*;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class JwtTckArchiveProcessor implements ApplicationArchiveProcessor {
    private static final String KEY_NAME = "mp.jwt.verify.publickey";

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (System.getProperty(KEY_NAME) != null) {
            System.clearProperty(KEY_NAME);
        }

        if (!(applicationArchive instanceof WebArchive)) {
            return;
        }

        WebArchive war = WebArchive.class.cast(applicationArchive);
        Node configProps = war.get("/META-INF/microprofile-config.properties");
        Node publicKeyNode = war.get("/WEB-INF/classes/publicKey.pem");
        Node publicKey4kNode = war.get("/WEB-INF/classes/publicKey4k.pem");

        if (configProps == null && publicKeyNode == null && publicKey4kNode == null) {
            return;
        }

        if (configProps == null) {
            if (publicKey4kNode != null) {
                addPublicKeyToEnv(publicKey4kNode);
            } else if (publicKeyNode != null) {
                addPublicKeyToEnv(publicKeyNode);
            }
        }
    }

    private void addPublicKeyToEnv(Node node) {
        StringBuilder keyText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(node.getAsset().openStream()))) {
            String line = reader.readLine();
            while (line != null) {
                keyText.append(line);
                keyText.append('\n');
                line = reader.readLine();
            }

            System.setProperty(KEY_NAME, keyText.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
