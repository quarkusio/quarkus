package io.quarkus.test.junit;

import java.lang.reflect.Method;

import org.junit.jupiter.api.function.Executable;

public class QuarkusExecutable implements Executable {

    private final Object executable;
    private final Method executeMethod;

    public QuarkusExecutable(Object executable) {
        this.executable = executable;
        try {
            this.executeMethod = executable.getClass().getMethod("execute");
            this.executeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void execute() throws Throwable {
        executeMethod.invoke(executable);
    }
}
