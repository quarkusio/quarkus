package io.quarkus.it.qute;

import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;

public class Brewery {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        Beer myBeer = new Beer();
        myBeer.name = "Pilsner";
        myBeer.completed = true;
        myBeer.done = true;
        myBeer.persist();
    }

}
