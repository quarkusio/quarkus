package io.quarkus.gcp.function.test;

import java.io.Writer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import io.quarkus.gcp.function.test.service.GreetingService;

@Named("httpTest")
@ApplicationScoped
public class HttpFunctionTest implements HttpFunction {
    @Inject
    GreetingService greetingService;

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Writer writer = httpResponse.getWriter();
        writer.write(greetingService.hello());
    }
}
