package io.quarkus.smallrye.jwt.deployment;

import static io.quarkus.smallrye.jwt.deployment.SmallRyeJwtProcessor.MP_JWT_VERIFY_KEY_LOCATION;

import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.smallrye.jwt.util.KeyUtils;

public class SmallryeJwtDevModeProcessor {

    private static final Logger LOGGER = Logger.getLogger(SmallryeJwtDevModeProcessor.class);

    private static final String MP_JWT_VERIFY_PUBLIC_KEY = "mp.jwt.verify.publickey";
    private static final String MP_JWT_VERIFY_ISSUER = "mp.jwt.verify.issuer";
    private static final String MP_JWT_DECRYPT_KEY_LOCATION = "mp.jwt.decrypt.key.location";

    private static final String SMALLRYE_JWT_NEW_TOKEN_ISSUER = "smallrye.jwt.new-token.issuer";
    private static final String SMALLRYE_JWT_SIGN_KEY_LOCATION = "smallrye.jwt.sign.key.location";
    private static final String SMALLRYE_JWT_SIGN_KEY = "smallrye.jwt.sign.key";
    private static final String SMALLRYE_JWT_ENCRYPT_KEY_LOCATION = "smallrye.jwt.encrypt.key.location";

    private static final String NONE = "NONE";
    private static final String DEFAULT_ISSUER = "https://quarkus.io/issuer";

    private static final int KEY_SIZE = 2048;

    private static final Set<String> JWT_SIGN_KEY_PROPERTIES = Set.of(
            MP_JWT_VERIFY_KEY_LOCATION,
            MP_JWT_VERIFY_PUBLIC_KEY,
            MP_JWT_DECRYPT_KEY_LOCATION,
            SMALLRYE_JWT_SIGN_KEY_LOCATION,
            SMALLRYE_JWT_SIGN_KEY,
            SMALLRYE_JWT_ENCRYPT_KEY_LOCATION);

    /**
     * This build step generates an RSA-256 key pair for development and test modes.
     * <p>
     * The key pair is generated only if the user has not set any of the {@code *.key} or {@code *.location} properties.
     * <p>
     * Additionally, if the user has not provided the {@code mp.jwt.verify.issuer} and {@code smallrye.jwt.new-token.issuer}
     * properties,
     * this build step will add a default issuer, regardless of the above condition.
     *
     * @throws NoSuchAlgorithmException if RSA-256 key generation fails.
     */
    @BuildStep(onlyIfNot = { IsNormal.class })
    void generateSignKeys(BuildProducer<DevServicesResultBuildItem> devServices,
            LiveReloadBuildItem liveReloadBuildItem) throws NoSuchAlgorithmException {

        Set<String> userProps = JWT_SIGN_KEY_PROPERTIES
                .stream()
                .filter(this::isConfigPresent)
                .collect(Collectors.toSet());

        if (!userProps.isEmpty()) {
            // If the user has set the property, we need to avoid adding or overriding it with the
            // smallrye default configuration
            Map<String, String> devServiceProps = addDefaultSmallryePropertiesIfMissing(userProps);

            if (!isConfigPresent(MP_JWT_VERIFY_ISSUER) && !isConfigPresent(SMALLRYE_JWT_NEW_TOKEN_ISSUER)) {
                devServiceProps.put(MP_JWT_VERIFY_ISSUER, DEFAULT_ISSUER);
                devServiceProps.put(SMALLRYE_JWT_NEW_TOKEN_ISSUER, DEFAULT_ISSUER);
            }

            devServices.produce(smallryeJwtDevServiceWith(devServiceProps));
            return;
        }

        KeyPairContext ctx = liveReloadBuildItem.getContextObject(KeyPairContext.class);

        LOGGER.info("The smallrye-jwt extension has configured an in-memory key pair, which is not enabled in production. " +
                "Please ensure the correct keys/locations are set in production to avoid potential issues.");
        if (ctx == null && !liveReloadBuildItem.isLiveReload()) {
            // first execution
            KeyPair keyPair = KeyUtils.generateKeyPair(KEY_SIZE);
            String publicKey = getStringKey(keyPair.getPublic());
            String privateKey = getStringKey(keyPair.getPrivate());

            Map<String, String> devServiceProps = generateDevServiceProperties(publicKey, privateKey);

            if (!isConfigPresent(MP_JWT_VERIFY_ISSUER) && !isConfigPresent(SMALLRYE_JWT_NEW_TOKEN_ISSUER)) {
                devServiceProps.put(MP_JWT_VERIFY_ISSUER, DEFAULT_ISSUER);
                devServiceProps.put(SMALLRYE_JWT_NEW_TOKEN_ISSUER, DEFAULT_ISSUER);
            }

            liveReloadBuildItem.setContextObject(KeyPairContext.class, new KeyPairContext(
                    devServiceProps));

            devServices.produce(smallryeJwtDevServiceWith(devServiceProps));
        }

        if (ctx != null && liveReloadBuildItem.isLiveReload()) {
            devServices.produce(smallryeJwtDevServiceWith(ctx.properties()));
        }
    }

    private Map<String, String> addDefaultSmallryePropertiesIfMissing(Set<String> userConfigs) {
        HashMap<String, String> devServiceConfigs = new HashMap<>();
        if (!userConfigs.contains(SMALLRYE_JWT_SIGN_KEY)) {
            devServiceConfigs.put(SMALLRYE_JWT_SIGN_KEY, NONE);
        }

        if (!userConfigs.contains(MP_JWT_VERIFY_PUBLIC_KEY)) {
            devServiceConfigs.put(MP_JWT_VERIFY_PUBLIC_KEY, NONE);
        }

        return devServiceConfigs;
    }

    private boolean isConfigPresent(String property) {
        return ConfigProvider.getConfig().getOptionalValue(property, String.class)
                .isPresent();
    }

    private DevServicesResultBuildItem smallryeJwtDevServiceWith(Map<String, String> properties) {
        return new DevServicesResultBuildItem(
                Feature.SMALLRYE_JWT.name(), null, properties);
    }

    private static Map<String, String> generateDevServiceProperties(String publicKey, String privateKey) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MP_JWT_VERIFY_PUBLIC_KEY, publicKey);
        properties.put(SMALLRYE_JWT_SIGN_KEY, privateKey);
        return properties;
    }

    private static String getStringKey(Key key) {
        return Base64.getEncoder()
                .encodeToString(key.getEncoded());
    }

    record KeyPairContext(Map<String, String> properties) {
    }
}
