package io.quarkus.arc.test.interceptors.exceptionhandling;

import jakarta.enterprise.context.Dependent;

@Dependent
public class ExceptionHandlingBean {

    public ExceptionHandlingBean() {
    }

    @ExceptionHandlingInterceptorBinding
    void foo(ExceptionHandlingCase exceptionHandlingCase) throws MyDeclaredException {
        switch (exceptionHandlingCase) {
            case DECLARED_EXCEPTION:
                throw new MyDeclaredException();
            case RUNTIME_EXCEPTION:
                throw new MyRuntimeException();
            case OTHER_EXCEPTIONS:
                // this case should be handled by the interceptor
                break;
        }
    }

    @ExceptionHandlingInterceptorBinding
    void bar() throws Exception {
        throw new Exception();
    }

    @ExceptionHandlingInterceptorBinding
    void baz() throws RuntimeException {
        throw new RuntimeException();
    }

    Integer fooNotIntercepted() {
        return 1;
    }
}
