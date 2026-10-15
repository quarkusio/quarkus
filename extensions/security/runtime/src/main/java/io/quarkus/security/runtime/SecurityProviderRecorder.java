package io.quarkus.security.runtime;

import static io.quarkus.security.runtime.SecurityProviderUtils.addProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.findProviderIndex;
import static io.quarkus.security.runtime.SecurityProviderUtils.insertProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.loadProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.loadProviderWithParams;

import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class SecurityProviderRecorder {

    private static final Logger LOG = Logger.getLogger(SecurityProviderRecorder.class);

    public void configureProvider(String providerName, List<String> providerConfigs) {
        Provider provider = Security.getProvider(providerName);
        if (provider == null) {
            throw new ConfigurationException(
                    String.format("Security provider '%s' is not available", providerName),
                    Set.of("quarkus.security.security-providers"));
        }
        for (String providerConfig : providerConfigs) {
            try {
                Provider configured = provider.configure(providerConfig);
                LOG.debugf("Registering security provider: %s (configured from %s)", configured.getName(), providerConfig);
                SecurityProviderUtils.addProvider(configured);
            } catch (Exception e) {
                throw new ConfigurationException(
                        String.format("Failed to configure security provider '%s'", providerName), e,
                        Set.of("quarkus.security.security-provider-config." + providerName));
            }
        }
    }

    public void addBouncyCastleProvider(boolean inFipsMode) {
        final String providerName = inFipsMode ? SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME
                : SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME;
        addProvider(loadProvider(providerName));
    }

    public void addBouncyCastleJsseProvider() {
        Provider bc = loadProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME);
        Provider bcJsse = loadProvider(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME);
        int sunJsseIndex = findProviderIndex(SecurityProviderUtils.SUN_JSSE_PROVIDER_NAME);
        insertProvider(bc, sunJsseIndex);
        insertProvider(bcJsse, sunJsseIndex + 1);
    }

    public void addBouncyCastleFipsJsseProvider() {
        Provider bc = loadProvider(SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME);
        int sunIndex = findProviderIndex(SecurityProviderUtils.SUN_PROVIDER_NAME);
        insertProvider(bc, sunIndex);
        Provider bcJsse = loadProviderWithParams(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME,
                new Class[] { boolean.class, Provider.class }, new Object[] { true, bc });
        insertProvider(bcJsse, sunIndex + 1);
    }
}
