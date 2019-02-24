package io.quarkus.camel.it.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.support.DefaultExchange;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:keep-alive")
                .id("timer")
                .setBody().constant("I'm alive !")
                .to("log:keep-alive");

        from("file:{{folder}}")
                .id("file")
                .setHeader(MyOrderService.class.getName(), MyOrderService::new)
                .split(body().tokenize("@"), CamelRoute.this::aggregate)
                // each splitted message is then send to this bean where we can process it
                .process(stateless(MyOrderService.class.getName(), "handleOrder"))
                // this is important to end the splitter route as we do not want to do more routing
                // on each splitted message
                .end()
                // after we have splitted and handled each message we want to send a single combined
                // response back to the original caller, so we let this bean build it for us
                // this bean will receive the result of the aggregate strategy: MyOrderStrategy
                .process(stateless(MyOrderService.class.getName(), "buildCombinedResponse"))
                // log out
                .to("log:out");

        from("netty4-http:http://0.0.0.0:8999/foo")
                .transform().constant("Netty Hello World");
    }

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        // put order together in old exchange by adding the order from new exchange
        List<String> orders;
        if (oldExchange != null) {
            orders = (List) oldExchange.getIn().getBody();
        } else {
            orders = new ArrayList<>();
            oldExchange = new DefaultExchange(newExchange.getContext());
            oldExchange.getIn().copyFromWithNewBody(newExchange.getIn(), orders);
        }
        String newLine = newExchange.getIn().getBody(String.class);

        log.debug("Aggregate old orders: " + orders);
        log.debug("Aggregate new order: " + newLine);

        // add orders to the list
        orders.add(newLine);

        // return old as this is the one that has all the orders gathered until now
        return oldExchange;
    }

    private Processor stateless(String header, String method) {
        return new AsyncProcessor() {
            @Override
            public boolean process(Exchange exchange, AsyncCallback callback) {
                return getBeanProcess(exchange).process(exchange, callback);
            }

            @Override
            public CompletableFuture<Exchange> processAsync(Exchange exchange) {
                return getBeanProcess(exchange).processAsync(exchange);
            }

            @Override
            public void process(Exchange exchange) throws Exception {
                getBeanProcess(exchange).process(exchange);
            }

            protected BeanProcessor getBeanProcess(Exchange exchange) {
                BeanProcessor bp = new BeanProcessor(
                        exchange.getIn().getHeader(header),
                        exchange.getContext());
                bp.setMethod(method);
                return bp;
            }
        };
    }

    @RegisterForReflection
    public class MyOrderService {

        private int counter;

        /**
         * We just handle the order by returning a id line for the order
         */
        public String handleOrder(String line) {
            log.debug("HandleOrder: " + line);
            return "(id=" + ++counter + ",item=" + line + ")";
        }

        /**
         * We use the same bean for building the combined response to send
         * back to the original caller
         */
        public Map<String, Object> buildCombinedResponse(List<String> lines) {
            log.debug("BuildCombinedResponse: " + lines);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("lines", lines);
            return result;
        }
    }

}
