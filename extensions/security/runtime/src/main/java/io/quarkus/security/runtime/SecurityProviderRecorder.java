package io.quarkus.security.runtime;

import static io.quarkus.security.runtime.SecurityProviderUtils.addProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.findProviderIndex;
import static io.quarkus.security.runtime.SecurityProviderUtils.insertProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.loadProvider;
import static io.quarkus.security.runtime.SecurityProviderUtils.loadProviderWithParams;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SecurityProviderRecorder {

    private static final Logger LOG = Logger.getLogger(SecurityProviderRecorder.class);

    public void addBouncyCastleProvider(boolean inFipsMode) {
        final String providerName = inFipsMode ? SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME
                : SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME;
        addProvider(loadProvider(providerName));
        if (inFipsMode) {
            setSecureRandomStrongAlgorithmIfNecessary();
        }
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
        setSecureRandomStrongAlgorithmIfNecessary();
    }

    private void setSecureRandomStrongAlgorithmIfNecessary() {
        try {
            // workaround for the issue on OpenJDK 17 & RHEL8 & FIPS
            // see https://github.com/bcgit/bc-java/issues/1285#issuecomment-2068958587
            // we can remove this when OpenJDK 17 support is dropped or if it starts working on newer versions of RHEL8+
            SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            SecureRandom secRandom = new SecureRandom();
            String origStrongAlgorithms = Security.getProperty("securerandom.strongAlgorithms");
            String usedAlgorithm = secRandom.getAlgorithm() + ":" + secRandom.getProvider().getName();
            String strongAlgorithms = origStrongAlgorithms == null ? usedAlgorithm : usedAlgorithm + "," + origStrongAlgorithms;
            LOG.debugf("Strong SecureRandom algorithm '%s' is not available. "
                    + "Using fallback algorithm '%s'.", origStrongAlgorithms, usedAlgorithm);
            Security.setProperty("securerandom.strongAlgorithms", strongAlgorithms);
        }
    }
}
