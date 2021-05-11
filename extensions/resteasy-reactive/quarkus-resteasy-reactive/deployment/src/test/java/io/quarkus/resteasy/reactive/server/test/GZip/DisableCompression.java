package io.quarkus.resteasy.reactive.server.test.GZip;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DisableCompression {
   // set default value of Transfer-Encoding as identity
   // String TransferEncoding() default "identity";
}
