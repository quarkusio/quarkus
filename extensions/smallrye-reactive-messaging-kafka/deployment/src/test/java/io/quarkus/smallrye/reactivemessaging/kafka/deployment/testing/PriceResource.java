package io.quarkus.smallrye.reactivemessaging.kafka.deployment.testing;

//
//import org.eclipse.microprofile.reactive.messaging.Channel;
//import org.reactivestreams.Publisher;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
//
//@Path("/prices")
public class PriceResource {
    //    private final Publisher<Double> processedPrices;
    //
    //    public PriceResource(@Channel("processed-prices") Publisher<Double> processedPrices) {
    //        this.processedPrices = processedPrices;
    //    }
    //
    //    @GET
    //    @Path("/stream")
    //    @Produces(MediaType.SERVER_SENT_EVENTS)
    //    public Publisher<Double> ssePrices() {
    //        return this.processedPrices;
    //    }
}