package io.quarkus.hibernate.orm.xml.orm;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusDevModeTest;

public class OrmXmlHotReloadImplicitFileTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(NonAnnotatedEntity.class)
                    .addClass(OrmXmlHotReloadTestResource.class)
                    .addAsResource("application-datasource-only.properties", "application.properties")
                    .addAsManifestResource("META-INF/persistence-mapping-file-implicit-orm-xml.xml", "persistence.xml")
                    // META-INF/orm.xml should be picked up even if it's not mentioned explicitly.
                    .addAsManifestResource("META-INF/orm-simple.xml", "orm.xml"));

    @Test
    public void changeOrmXml() {
        assertThat(getColumnNames())
                .contains("thename")
                .doesNotContain("name", "thename2");

        TEST.modifyResourceFile("META-INF/orm.xml",
                s -> s.replace("<column name=\"thename\" />", "<column name=\"thename2\" />"));

        assertThat(getColumnNames())
                .contains("thename2")
                .doesNotContain("name", "thename");
    }

    private String[] getColumnNames() {
        return when().get("/orm-xml-hot-reload-test/column-names")
                .then().extract().body().asString()
                .split("\n");
    }

}
