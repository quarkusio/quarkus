package io.quarkus.elytron.security.ldap;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.wildfly.security.auth.realm.ldap.DirContextFactory;
import org.wildfly.security.manager.action.SetContextClassLoaderAction;

public class QuarkusDirContextFactory implements DirContextFactory {
    //    private static final ElytronMessages log = Logger.getMessageLogger(ElytronMessages.class, "org.wildfly.security");

    private static final String CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";
    private static final String READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";
    public static final String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private static final String SECURITY_AUTHENTICATION = "simple";

    private static final String DEFAULT_CONNECT_TIMEOUT = "5000"; // ms
    private static final String DEFAULT_READ_TIMEOUT = "60000"; // ms

    private final String providerUrl;
    private final String securityPrincipal;
    private final String securityCredential;
    private final ClassLoader targetClassLoader;

    public QuarkusDirContextFactory(String providerUrl, String securityPrincipal, String securityCredential) {
        this.providerUrl = providerUrl;
        this.securityPrincipal = securityPrincipal;
        this.securityCredential = securityCredential;
        this.targetClassLoader = getClass().getClassLoader();
    }

    @Override
    public DirContext obtainDirContext(ReferralMode mode) throws NamingException {
        char[] charPassword = null;
        if (securityCredential != null) { // password from String
            charPassword = securityCredential.toCharArray();
        }
        return createDirContext(securityPrincipal, charPassword, mode);
    }

    @Override
    public DirContext obtainDirContext(CallbackHandler handler, ReferralMode mode) throws NamingException {
        NameCallback nameCallback = new NameCallback("Principal Name");
        PasswordCallback passwordCallback = new PasswordCallback("Password", false);

        try {
            handler.handle(new Callback[] { nameCallback, passwordCallback });
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain credential", e);
            //            throw log.couldNotObtainCredentialWithCause(e);
        }

        String securityPrincipal = nameCallback.getName();

        if (securityPrincipal == null) {
            throw new RuntimeException("Could not obtain principal");
            //            throw log.couldNotObtainPrincipal();
        }

        char[] securityCredential = passwordCallback.getPassword();

        if (securityCredential == null) {
            throw new RuntimeException("Could not obtain credential");
            //            throw log.couldNotObtainCredential();
        }

        return createDirContext(securityPrincipal, securityCredential, mode);
    }

    private DirContext createDirContext(String securityPrincipal, char[] securityCredential, ReferralMode mode)
            throws NamingException {
        final ClassLoader oldClassLoader = setClassLoaderTo(targetClassLoader);
        try {
            Hashtable<String, Object> env = new Hashtable<>();

            env.put(InitialDirContext.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
            env.put(InitialDirContext.PROVIDER_URL, providerUrl);
            env.put(InitialDirContext.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
            if (securityPrincipal != null) {
                env.put(InitialDirContext.SECURITY_PRINCIPAL, securityPrincipal);
            }
            if (securityCredential != null) {
                env.put(InitialDirContext.SECURITY_CREDENTIALS, securityCredential);
            }
            env.put(InitialDirContext.REFERRAL, mode == null ? ReferralMode.IGNORE.getValue() : mode.getValue());
            env.put(CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
            env.put(READ_TIMEOUT, DEFAULT_READ_TIMEOUT);

            //            if (log.isDebugEnabled()) {
            //                log.debugf("Creating [" + InitialDirContext.class + "] with environment:");
            //                env.forEach((key, value) -> log.debugf("    Property [%s] with value [%s]", key,
            //                        key != InitialDirContext.SECURITY_CREDENTIALS ? Arrays2.objectToString(value) : "******"));
            //            }

            InitialLdapContext initialContext;

            try {
                initialContext = new InitialLdapContext(env, null);
            } catch (NamingException ne) {
                //                log.debugf(ne, "Could not create [%s]. Failed to connect to LDAP server.", InitialLdapContext.class);
                throw ne;
            }

            //            log.debugf("[%s] successfully created. Connection established to LDAP server.", initialContext);

            return new DelegatingLdapContext(initialContext, this::returnContext, null);
        } finally {
            setClassLoaderTo(oldClassLoader);
        }
    }

    @Override
    public void returnContext(DirContext context) {

        if (context == null) {
            return;
        }

        if (context instanceof InitialDirContext) {
            final ClassLoader oldClassLoader = setClassLoaderTo(targetClassLoader);
            try {
                context.close();
                //                log.debugf("Context [%s] was closed. Connection closed or just returned to the pool.", context);
            } catch (NamingException ignored) {
            } finally {
                setClassLoaderTo(oldClassLoader);
            }
        }
    }

    private ClassLoader setClassLoaderTo(final ClassLoader targetClassLoader) {
        return doPrivileged(new SetContextClassLoaderAction(targetClassLoader));
    }

    private static <T> T doPrivileged(final PrivilegedAction<T> action) {
        return System.getSecurityManager() != null ? AccessController.doPrivileged(action) : action.run();
    }
}
