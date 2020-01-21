package io.quarkus.it.vault;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.SigningInput;

@ApplicationScoped
public class VaultTestService {

    private static final Logger log = Logger.getLogger(VaultTestService.class);

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "password")
    String someSecret;

    @Inject
    VaultKVSecretEngine kv;

    @Inject
    VaultTransitSecretEngine transit;

    @Transactional
    public String test() {

        String expectedPassword = "bar";
        if (!expectedPassword.equals(someSecret)) {
            return "someSecret=" + someSecret + "; expected: " + expectedPassword;
        }
        String password = ConfigProviderResolver.instance().getConfig().getValue("password", String.class);
        if (!expectedPassword.equals(password)) {
            return "password=" + password + "; expected: " + expectedPassword;
        }

        Map<String, String> secrets = kv.readSecret("foo");
        String expectedSecrets = "{secret=s\u20accr\u20act}";
        if (!expectedSecrets.equals(secrets.toString())) {
            return "/foo=" + secrets + "; expected: " + expectedSecrets;
        }

        try {
            List gifts = entityManager.createQuery("select g from Gift g").getResultList();
            int count = gifts.size();
            log.info("found " + count + " gifts");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter printWriter = new PrintWriter(sw);
            e.printStackTrace(printWriter);
            return sw.toString();
        }

        String coucou = "coucou";
        SigningInput input = new SigningInput(coucou);
        String keyName = "my-encryption-key";
        String ciphertext = transit.encrypt(keyName, coucou);
        ClearData decrypted = transit.decrypt(keyName, ciphertext);
        if (!coucou.equals(decrypted.asString())) {
            return "decrypted=" + password + "; expected: " + coucou;
        }

        String rewraped = transit.rewrap(keyName, ciphertext, null);
        decrypted = transit.decrypt(keyName, rewraped);
        if (!coucou.equals(decrypted.asString())) {
            return "decrypted=" + password + "; expected: " + coucou;
        }

        String signature = transit.sign("my-sign-key", input, null);
        if (!signature.startsWith("vault:v1:")) {
            return "invalid signature " + signature;
        }

        transit.verifySignature("my-sign-key", signature, input, null);

        return "OK";
    }

}
