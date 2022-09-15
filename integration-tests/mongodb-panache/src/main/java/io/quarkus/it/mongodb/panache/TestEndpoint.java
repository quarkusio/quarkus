package io.quarkus.it.mongodb.panache;

import java.lang.reflect.Method;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

@Path("/accessors")
public class TestEndpoint {

    @GET
    public String testAccessors() throws NoSuchMethodException, SecurityException {
        checkMethod(AccessorEntity.class, "getString", String.class);
        checkMethod(AccessorEntity.class, "isBool", boolean.class);
        checkMethod(AccessorEntity.class, "getC", char.class);
        checkMethod(AccessorEntity.class, "getS", short.class);
        checkMethod(AccessorEntity.class, "getI", int.class);
        checkMethod(AccessorEntity.class, "getL", long.class);
        checkMethod(AccessorEntity.class, "getF", float.class);
        checkMethod(AccessorEntity.class, "getD", double.class);
        checkMethod(AccessorEntity.class, "getT", Object.class);
        checkMethod(AccessorEntity.class, "getT2", Object.class);

        checkMethod(AccessorEntity.class, "setString", void.class, String.class);
        checkMethod(AccessorEntity.class, "setBool", void.class, boolean.class);
        checkMethod(AccessorEntity.class, "setC", void.class, char.class);
        checkMethod(AccessorEntity.class, "setS", void.class, short.class);
        checkMethod(AccessorEntity.class, "setI", void.class, int.class);
        checkMethod(AccessorEntity.class, "setL", void.class, long.class);
        checkMethod(AccessorEntity.class, "setF", void.class, float.class);
        checkMethod(AccessorEntity.class, "setD", void.class, double.class);
        checkMethod(AccessorEntity.class, "setT", void.class, Object.class);
        checkMethod(AccessorEntity.class, "setT2", void.class, Object.class);

        try {
            checkMethod(AccessorEntity.class, "getTrans2", Object.class);
            Assertions.fail("transient field should have no getter: trans2");
        } catch (NoSuchMethodException x) {
        }

        try {
            checkMethod(AccessorEntity.class, "setTrans2", void.class, Object.class);
            Assertions.fail("transient field should have no setter: trans2");
        } catch (NoSuchMethodException x) {
        }

        // Now check that accessors are called
        AccessorEntity entity = new AccessorEntity();
        @SuppressWarnings("unused")
        byte b = entity.b;
        Assertions.assertEquals(1, entity.getBCalls);
        entity.i = 2;
        Assertions.assertEquals(1, entity.setICalls);
        Object trans = entity.trans;
        Assertions.assertEquals(0, entity.getTransCalls);
        entity.trans = trans;
        Assertions.assertEquals(0, entity.setTransCalls);

        // accessors inside the entity itself
        entity.method();
        Assertions.assertEquals(2, entity.getBCalls);
        Assertions.assertEquals(2, entity.setICalls);

        return "OK";
    }

    private void checkMethod(Class<?> klass, String name, Class<?> returnType, Class<?>... params)
            throws NoSuchMethodException, SecurityException {
        Method method = klass.getMethod(name, params);
        Assertions.assertEquals(returnType, method.getReturnType());
    }
}
