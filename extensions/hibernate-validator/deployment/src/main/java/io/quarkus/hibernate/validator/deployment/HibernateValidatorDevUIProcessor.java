package io.quarkus.hibernate.validator.deployment;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.page.CardPageBuildItem;

@BuildSteps(onlyIf = { IsLocalDevelopment.class })
public class HibernateValidatorDevUIProcessor {

    @BuildStep
    public CardPageBuildItem create() {
        CardPageBuildItem card = new CardPageBuildItem();
        card.setLogo("hibernate_icon_dark.svg", "hibernate_icon_light.svg");
        card.addLibraryVersion("org.hibernate.validator", "hibernate-validator", "Hibernate Validator",
                "https://hibernate.org/validator/");
        card.addLibraryVersion("jakarta.validation", "jakarta.validation-api", "Jakarta Validation",
                "https://beanvalidation.org/");
        return card;
    }

}
