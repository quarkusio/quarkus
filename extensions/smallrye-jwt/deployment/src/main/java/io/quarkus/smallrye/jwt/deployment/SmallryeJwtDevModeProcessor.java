package io.quarkus.smallrye.jwt.deployment;

import static io.quarkus.smallrye.jwt.deployment.SmallRyeJwtProcessor.MP_JWT_VERIFY_KEY_LOCATION;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.smallrye.jwt.util.KeyUtils;

public class SmallryeJwtDevModeProcessor {

    private static final Logger LOGGER = Logger.getLogger(SmallryeJwtDevModeProcessor.class);

    public static final String MP_JWT_VERIFY_PUBLIC_KEY = "mp.jwt.verify.publickey";
    private static final String MP_JWT_VERIFY_ISSUER = "mp.jwt.verify.issuer";
    private static final String SMALLRYE_JWT_DECRYPT_KEY = "smallrye.jwt.decrypt.key"; // no MP equivalent
    private static final String MP_JWT_DECRYPT_KEY_LOCATION = "mp.jwt.decrypt.key.location";

    private static final String SMALLRYE_JWT_NEW_TOKEN_ISSUER = "smallrye.jwt.new-token.issuer";
    private static final String SMALLRYE_JWT_SIGN_KEY_LOCATION = "smallrye.jwt.sign.key.location";
    public static final String SMALLRYE_JWT_SIGN_KEY = "smallrye.jwt.sign.key";
    private static final String SMALLRYE_JWT_ENCRYPT_KEY = "smallrye.jwt.encrypt.key";
    private static final String SMALLRYE_JWT_ENCRYPT_KEY_LOCATION = "smallrye.jwt.encrypt.key.location";

    private static final String NONE = "NONE";
    private static final String DEFAULT_ISSUER = "https://quarkus.io/issuer";

    private static final int KEY_SIZE = 2048;

    private static final Set<String> JWT_SIGN_KEY_PROPERTIES = Set.of(
            MP_JWT_VERIFY_KEY_LOCATION,
            MP_JWT_VERIFY_PUBLIC_KEY,
            SMALLRYE_JWT_DECRYPT_KEY,
            MP_JWT_DECRYPT_KEY_LOCATION,
            SMALLRYE_JWT_SIGN_KEY_LOCATION,
            SMALLRYE_JWT_SIGN_KEY,
            SMALLRYE_JWT_ENCRYPT_KEY,
            SMALLRYE_JWT_ENCRYPT_KEY_LOCATION);
    public static final String DEV_PRIVATE_KEY_PEM = "dev.privateKey.pem";
    public static final String DEV_PUBLIC_KEY_PEM = "dev.publicKey.pem";

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
     * @throws IOException if persistent key storage fails
     */
    @BuildStep(onlyIfNot = { IsNormal.class })
    void generateSignKeys(BuildProducer<DevServicesResultBuildItem> devServices,
            LiveReloadBuildItem liveReloadBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            Optional<GeneratePersistentDevModeJwtKeysBuildItem> generatePersistentDevModeJwtKeysBuildItem,
            Optional<GenerateEncryptedDevModeJwtKeysBuildItem> generateEncryptedDevModeJwtKeysBuildItem)
            throws GeneralSecurityException, IOException {

        Set<String> userProps = JWT_SIGN_KEY_PROPERTIES
                .stream()
                .filter(this::isConfigPresent)
                .collect(Collectors.toSet());

        if (!userProps.isEmpty()) {
            // If the user has set the property, we need to avoid adding or overriding it with the
            // smallrye default configuration
            Map<String, String> devServiceProps = addDefaultSmallryePropertiesIfMissing(userProps,
                    generateEncryptedDevModeJwtKeysBuildItem);

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
            KeyPair keyPair = generateOrReloadKeyPair(curateOutcomeBuildItem, generatePersistentDevModeJwtKeysBuildItem);
            String publicKey = getStringKey(keyPair.getPublic());
            String privateKey = getStringKey(keyPair.getPrivate());

            Map<String, String> devServiceProps = generateDevServiceProperties(publicKey, privateKey,
                    generateEncryptedDevModeJwtKeysBuildItem);

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

    private KeyPair generateOrReloadKeyPair(CurateOutcomeBuildItem curateOutcomeBuildItem,
            Optional<GeneratePersistentDevModeJwtKeysBuildItem> generatePersistentDevModeJwtKeysBuildItem)
            throws GeneralSecurityException, IOException {
        if (generatePersistentDevModeJwtKeysBuildItem.isPresent()) {
            File buildDir = getBuildDir(curateOutcomeBuildItem);

            buildDir.mkdirs();
            File privateKey = new File(buildDir, DEV_PRIVATE_KEY_PEM);
            File publicKey = new File(buildDir, DEV_PUBLIC_KEY_PEM);
            if (!privateKey.exists() || !publicKey.exists()) {
                KeyPair keyPair = KeyUtils.generateKeyPair(KEY_SIZE);
                LOGGER.infof("Generating private/public keys for DEV/TEST in %s and %s", privateKey, publicKey);
                try (FileWriter fw = new FileWriter(privateKey)) {
                    fw.append("-----BEGIN PRIVATE KEY-----\n");
                    fw.append(Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
                    fw.append("\n");
                    fw.append("-----END PRIVATE KEY-----\n");
                }
                try (FileWriter fw = new FileWriter(publicKey)) {
                    fw.append("-----BEGIN PUBLIC KEY-----\n");
                    fw.append(Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded()));
                    fw.append("\n");
                    fw.append("-----END PUBLIC KEY-----\n");
                }
                return keyPair;
            } else {
                // read from disk
                return new KeyPair(KeyUtils.readPublicKey(publicKey.getName()),
                        KeyUtils.readPrivateKey(privateKey.getName()));
            }
        } else {
            return KeyUtils.generateKeyPair(KEY_SIZE);
        }
    }

    public static File getBuildDir(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        File buildDir = null;
        ArtifactSources src = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getSources();
        if (src != null) { // shouldn't be null in dev mode
            Collection<SourceDir> srcDirs = src.getResourceDirs();
            if (srcDirs.isEmpty()) {
                // if the module has no resources dir?
                srcDirs = src.getSourceDirs();
            }
            if (!srcDirs.isEmpty()) {
                // pick the first resources output dir
                Path resourcesOutputDir = srcDirs.iterator().next().getOutputDir();
                buildDir = resourcesOutputDir.toFile();
            }
        }
        if (buildDir == null) {
            // the module doesn't have any sources nor resources, stick to the build dir
            buildDir = new File(
                    curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getWorkspaceModule().getBuildDir(),
                    "classes");
        }
        return buildDir;
    }

    private Map<String, String> addDefaultSmallryePropertiesIfMissing(Set<String> userConfigs,
            Optional<GenerateEncryptedDevModeJwtKeysBuildItem> generateEncryptedDevModeJwtKeysBuildItem) {
        HashMap<String, String> devServiceConfigs = new HashMap<>();
        if (!userConfigs.contains(SMALLRYE_JWT_SIGN_KEY)) {
            devServiceConfigs.put(SMALLRYE_JWT_SIGN_KEY, NONE);
        }

        if (!userConfigs.contains(MP_JWT_VERIFY_PUBLIC_KEY)) {
            devServiceConfigs.put(MP_JWT_VERIFY_PUBLIC_KEY, NONE);
        }

        if (generateEncryptedDevModeJwtKeysBuildItem.isPresent()) {
            if (!userConfigs.contains(SMALLRYE_JWT_ENCRYPT_KEY) && !userConfigs.contains(SMALLRYE_JWT_ENCRYPT_KEY_LOCATION)) {
                devServiceConfigs.put(SMALLRYE_JWT_ENCRYPT_KEY, NONE);
            }

            if (!userConfigs.contains(SMALLRYE_JWT_DECRYPT_KEY) && !userConfigs.contains(MP_JWT_DECRYPT_KEY_LOCATION)) {
                devServiceConfigs.put(SMALLRYE_JWT_DECRYPT_KEY, NONE);
            }
        }

        return devServiceConfigs;
    }

    private boolean isConfigPresent(String property) {
        return ConfigProvider.getConfig().getOptionalValue(property, String.class)
                .isPresent();
    }

    private DevServicesResultBuildItem smallryeJwtDevServiceWith(Map<String, String> properties) {
        return DevServicesResultBuildItem.discovered()
                .feature(Feature.SMALLRYE_JWT)
                .config(properties)
                .build();
    }

    private static Map<String, String> generateDevServiceProperties(String publicKey, String privateKey,
            Optional<GenerateEncryptedDevModeJwtKeysBuildItem> generateEncryptedDevModeJwtKeysBuildItem) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(MP_JWT_VERIFY_PUBLIC_KEY, publicKey);
        properties.put(SMALLRYE_JWT_SIGN_KEY, privateKey);
        if (generateEncryptedDevModeJwtKeysBuildItem.isPresent()) {
            properties.put(SMALLRYE_JWT_ENCRYPT_KEY, publicKey);
            properties.put(SMALLRYE_JWT_DECRYPT_KEY, privateKey);
        }
        return properties;
    }

    private static String getStringKey(Key key) {
        return Base64.getEncoder()
                .encodeToString(key.getEncoded());
    }

    record KeyPairContext(Map<String, String> properties) {
    }
}
