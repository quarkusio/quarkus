package io.quarkus.kafka.client.runtime.graal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.spi.LoginModule;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.auth.SaslExtensions;
import org.apache.kafka.common.security.auth.SaslExtensionsCallback;
import org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler;
import org.apache.kafka.common.security.scram.ScramExtensionsCallback;
import org.apache.kafka.common.security.scram.internals.ScramMechanism;
import org.apache.kafka.common.utils.Utils;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.KeepOriginal;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = SaslClientCallbackHandler.class)
final class Target_org_apache_kafka_common_security_authenticator_SaslClientCallbackHandler
        implements AuthenticateCallbackHandler {

    @Alias
    @RecomputeFieldValue(kind = Kind.None)
    String mechanism;

    @Inject
    @RecomputeFieldValue(kind = Kind.None)
    Subject subject;

    @Inject
    @RecomputeFieldValue(kind = Kind.None)
    Map<String, ?> sharedState;

    @Override
    @Substitute
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        mechanism = saslMechanism;

        for (AppConfigurationEntry entry : jaasConfigEntries) {
            try {
                if (subject == null) {
                    subject = new Subject();
                }
                if (sharedState == null) {
                    sharedState = new HashMap<>();
                }
                LoginModule result = Utils.newInstance(entry.getLoginModuleName(), LoginModule.class);
                result.initialize(subject, this, sharedState, entry.getOptions());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @Substitute
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nc = (NameCallback) callback;
                if (subject != null && !subject.getPublicCredentials(String.class).isEmpty()) {
                    nc.setName(subject.getPublicCredentials(String.class).iterator().next());
                } else {
                    nc.setName(nc.getDefaultName());
                }
            } else if (callback instanceof PasswordCallback) {
                if (subject != null && !subject.getPrivateCredentials(String.class).isEmpty()) {
                    char[] password = subject.getPrivateCredentials(String.class).iterator().next()
                            .toCharArray();
                    ((PasswordCallback) callback).setPassword(password);
                } else {
                    String errorMessage = "Could not login: the client is being asked for a password, but the Kafka" +
                            " client code does not currently support obtaining a password from the user.";
                    throw new UnsupportedCallbackException(callback, errorMessage);
                }
            } else if (callback instanceof RealmCallback) {
                RealmCallback rc = (RealmCallback) callback;
                rc.setText(rc.getDefaultText());
            } else if (callback instanceof AuthorizeCallback) {
                AuthorizeCallback ac = (AuthorizeCallback) callback;
                String authId = ac.getAuthenticationID();
                String authzId = ac.getAuthorizationID();
                ac.setAuthorized(authId.equals(authzId));
                if (ac.isAuthorized()) {
                    ac.setAuthorizedID(authzId);
                }
            } else if (callback instanceof ScramExtensionsCallback) {
                if (ScramMechanism.isScram(mechanism) && subject != null && !subject
                        .getPublicCredentials(Map.class).isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> extensions = (Map<String, String>) subject
                            .getPublicCredentials(Map.class).iterator().next();
                    ((ScramExtensionsCallback) callback).extensions(extensions);
                }
            } else if (callback instanceof SaslExtensionsCallback) {
                if (!SaslConfigs.GSSAPI_MECHANISM.equals(mechanism) &&
                        subject != null && !subject.getPublicCredentials(SaslExtensions.class).isEmpty()) {
                    SaslExtensions extensions = subject.getPublicCredentials(SaslExtensions.class).iterator()
                            .next();
                    ((SaslExtensionsCallback) callback).extensions(extensions);
                }
            } else {
                throw new UnsupportedCallbackException(callback, "Unrecognized SASL ClientCallback");
            }
        }
    }

    @Override
    @Substitute
    @KeepOriginal
    public void close() {

    }

}
