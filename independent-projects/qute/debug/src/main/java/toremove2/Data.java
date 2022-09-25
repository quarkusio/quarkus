package toremove2;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

public class Data {

    private static String HELLO_TEMPLATE = "test";

    public static void main(String[] args) {
        Engine engine0 = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Template template = engine.parse("{abcd}! \r\n {abcd}", null, HELLO_TEMPLATE);
        System.err.println(engine.getTemplate(HELLO_TEMPLATE));

        while (true) {
            TemplateInstance instance = template.data("abcd", HELLO_TEMPLATE);
            Object data = instance.getAttribute("qute$rootContext");
            String s = instance.data("abcd", HELLO_TEMPLATE).render();
            System.err.println(s);

        }
    }
}
