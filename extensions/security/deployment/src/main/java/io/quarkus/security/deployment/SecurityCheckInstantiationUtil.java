package io.quarkus.security.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;

import java.util.function.Function;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;

public class SecurityCheckInstantiationUtil {

    private SecurityCheckInstantiationUtil() {
    }

    public static Function<BytecodeCreator, ResultHandle> rolesAllowedSecurityCheck(final String[] rolesAllowed) {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                if ((rolesAllowed == null)) {
                    throw new IllegalStateException(
                            "Cannot use a null array to create an instance of " + RolesAllowedCheck.class.getName());
                }

                ResultHandle ctorArgs = creator.newArray(String.class, creator.load(rolesAllowed.length));
                int i = 0;
                for (String val : rolesAllowed) {
                    creator.writeArrayValue(ctorArgs, i++, creator.load(val));
                }
                return creator.newInstance(ofConstructor(RolesAllowedCheck.class, String[].class), ctorArgs);
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> denyAllSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.newInstance(ofConstructor(DenyAllCheck.class));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> permitAllSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.newInstance(ofConstructor(PermitAllCheck.class));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> authenticatedSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.newInstance(ofConstructor(AuthenticatedCheck.class));
            }
        };
    }
}
