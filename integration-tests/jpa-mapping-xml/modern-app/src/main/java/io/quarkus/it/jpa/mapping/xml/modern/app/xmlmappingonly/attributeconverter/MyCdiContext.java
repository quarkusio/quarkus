package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.attributeconverter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyCdiContext {

    public static void checkAvailable(MyCdiContext injected) {
        assertThat(injected)
                .as("CDI context should be available")
                .isNotNull()
                .returns(true, MyCdiContext::worksProperly);
    }

    public boolean worksProperly() {
        return true;
    }
}
