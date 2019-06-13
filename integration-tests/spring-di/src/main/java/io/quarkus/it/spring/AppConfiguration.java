package io.quarkus.it.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean(name = "cap")
    public StringFunction capitalizer(@Qualifier("dumb") Dummy notUsedJustMakingSureDIInMethodsWorks,
            OtherDummy alsoNotUsed) {
        return String::toUpperCase;
    }
}
