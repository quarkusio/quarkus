package io.quarkus.it.pulsar;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.apache.pulsar.client.api.CryptoKeyReader;
import org.apache.pulsar.client.api.EncryptionKeyInfo;
import org.apache.pulsar.client.impl.conf.ConsumerConfigurationData;
import org.apache.pulsar.client.impl.conf.ProducerConfigurationData;

import io.smallrye.common.annotation.Identifier;

@Singleton
public class PulsarConfig {

    @Produces
    @Identifier("fruits-out")
    @ApplicationScoped
    public ProducerConfigurationData producer() {
        ProducerConfigurationData data = new ProducerConfigurationData();
        data.setCryptoKeyReader(new RawFileKeyReader("test_ecdsa_pubkey.pem", "test_ecdsa_privkey.pem"));
        data.setEncryptionKeys(Set.of("myappkey"));
        return data;
    }

    @Produces
    @Identifier("fruits-in")
    @ApplicationScoped
    public ConsumerConfigurationData<Object> consumer() {
        ConsumerConfigurationData<Object> data = new ConsumerConfigurationData<>();
        data.setCryptoKeyReader(new RawFileKeyReader("test_ecdsa_pubkey.pem", "test_ecdsa_privkey.pem"));
        return data;
    }

    class RawFileKeyReader implements CryptoKeyReader {

        String publicKeyFile = "";
        String privateKeyFile = "";

        RawFileKeyReader(String pubKeyFile, String privKeyFile) {
            publicKeyFile = pubKeyFile;
            privateKeyFile = privKeyFile;
        }

        @Override
        public EncryptionKeyInfo getPublicKey(String keyName, Map<String, String> keyMeta) {
            EncryptionKeyInfo keyInfo = new EncryptionKeyInfo();
            try {
                keyInfo.setKey(PulsarConfig.class.getResourceAsStream("/" + publicKeyFile).readAllBytes());
            } catch (IOException e) {
                System.out.println("ERROR: Failed to read public key from file " + publicKeyFile);
                e.printStackTrace();
            }
            return keyInfo;
        }

        @Override
        public EncryptionKeyInfo getPrivateKey(String keyName, Map<String, String> keyMeta) {
            EncryptionKeyInfo keyInfo = new EncryptionKeyInfo();
            try {
                keyInfo.setKey(PulsarConfig.class.getResourceAsStream("/" + privateKeyFile).readAllBytes());
            } catch (IOException e) {
                System.out.println("ERROR: Failed to read private key from file " + privateKeyFile);
                e.printStackTrace();
            }
            return keyInfo;
        }
    }
}
