package io.quarkus.kafka.client.runtime.graal;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.concurrent.TimeUnit;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "javax.security.auth.Subject")
final class Target_javax_security_auth_Subject {

    @Substitute
    public static <T> T doAs(final Subject subject,
            final java.security.PrivilegedExceptionAction<T> action)
            throws java.security.PrivilegedActionException {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.DO_AS_PERMISSION);
        }
        if (action == null) {
            throw new NullPointerException("Invalid null action provided");
        }

        final AccessControlContext currentAcc = AccessController.getContext();

        SubjectHolder.subjects.put(currentAcc, subject);

        return java.security.AccessController.doPrivileged(action,
                createContext(subject, currentAcc));
    }

    @Substitute
    public static Subject getSubject(final AccessControlContext acc) {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.GET_SUBJECT_PERMISSION);
        }

        if (acc == null) {
            throw new NullPointerException("Invalid null AccessControlContext provided");
        }

        // return the Subject from the DomainCombiner of the provided context
        return AccessController.doPrivileged(new java.security.PrivilegedAction<Subject>() {
            public Subject run() {
                return SubjectHolder.subjects.getIfPresent(acc);
            }
        });
    }

    @Substitute
    private static AccessControlContext createContext(final Subject subject,
            final AccessControlContext acc) {

        return java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<AccessControlContext>() {
            public AccessControlContext run() {
                if (subject == null)
                    return new AccessControlContext(acc, null);
                else
                    return new AccessControlContext(acc,
                            new SubjectDomainCombiner(subject));
            }
        });
    }

    final static class AuthPermissionHolder {
        static final AuthPermission DO_AS_PERMISSION = new AuthPermission("doAs");

        static final AuthPermission DO_AS_PRIVILEGED_PERMISSION = new AuthPermission("doAsPrivileged");

        static final AuthPermission SET_READ_ONLY_PERMISSION = new AuthPermission("setReadOnly");

        static final AuthPermission GET_SUBJECT_PERMISSION = new AuthPermission("getSubject");

        static final AuthPermission MODIFY_PRINCIPALS_PERMISSION = new AuthPermission("modifyPrincipals");

        static final AuthPermission MODIFY_PUBLIC_CREDENTIALS_PERMISSION = new AuthPermission("modifyPublicCredentials");

        static final AuthPermission MODIFY_PRIVATE_CREDENTIALS_PERMISSION = new AuthPermission("modifyPrivateCredentials");
    }

    final static class SubjectHolder {
        static final Cache<AccessControlContext, Subject> subjects = Caffeine.newBuilder()
                .initialCapacity(20)
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build();
    }
}
