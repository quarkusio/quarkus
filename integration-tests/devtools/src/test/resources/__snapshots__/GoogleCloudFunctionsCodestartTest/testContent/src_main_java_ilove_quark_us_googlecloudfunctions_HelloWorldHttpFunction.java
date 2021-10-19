package ilove.quark.us.googlecloudfunctions;

import java.io.Writer;

import javax.enterprise.context.ApplicationScoped;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

@ApplicationScoped
public class HelloWorldHttpFunction implements HttpFunction {

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Writer writer = httpResponse.getWriter();
        writer.write("Hello World");
    }
}