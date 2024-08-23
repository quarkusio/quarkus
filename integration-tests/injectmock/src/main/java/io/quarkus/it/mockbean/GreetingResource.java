package io.quarkus.it.mockbean;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("greeting")
public class GreetingResource {

    final MessageService messageService;
    final SuffixService suffixService;
    final CapitalizerService capitalizerService;

    public GreetingResource(MessageService messageService, SuffixService suffixService, CapitalizerService capitalizerService) {
        this.messageService = messageService;
        this.suffixService = suffixService;
        this.capitalizerService = capitalizerService;
    }

    @GET
    @Produces("text/plain")
    public String greet() {
        return capitalizerService.capitalize(messageService.getMessage() + suffixService.getSuffix());
    }
}
