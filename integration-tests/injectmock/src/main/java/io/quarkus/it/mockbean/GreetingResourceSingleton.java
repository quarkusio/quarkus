package io.quarkus.it.mockbean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("greetingSingleton")
public class GreetingResourceSingleton {

    final MessageServiceSingleton messageService;
    final SuffixServiceSingleton suffixService;
    final CapitalizerServiceSingleton capitalizerService;

    public GreetingResourceSingleton(MessageServiceSingleton messageService, SuffixServiceSingleton suffixService,
            CapitalizerServiceSingleton capitalizerService) {
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
