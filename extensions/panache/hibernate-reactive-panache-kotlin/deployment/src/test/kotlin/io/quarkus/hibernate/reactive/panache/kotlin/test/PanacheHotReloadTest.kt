package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.quarkus.test.QuarkusDevModeTest
import io.restassured.RestAssured
import org.hamcrest.Matchers
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class PanacheHotReloadTest {
    @RegisterExtension
    val TEST = QuarkusDevModeTest()
        .withApplicationRoot { jar: JavaArchive ->
            jar
                .addClasses(
                    MyTestEntity::class.java,
                    MyTestResource::class.java,
                )
                .addAsResource("application.properties")
                .addAsResource("import.sql")
        }

    @Test
    fun testAddNewFieldToEntity() {
        val expectedName = "{\"id\":1,\"name\":\"my name\"}"
        assertBodyIs(expectedName)
        TEST.modifySourceFile(MyTestEntity::class.java) { s ->
            s.replace(
                "public String name;",
                "public String name;public String tag;"
            )
        }
        TEST.modifyResourceFile(
            "import.sql"
        ) { s -> s.replace(";", ";\nUPDATE MyEntity SET tag = 'related' WHERE id = 1;\n") }
        val hotReloadExpectedName = "{\"id\":1,\"name\":\"my name\",\"tag\":\"related\"}"
        assertBodyIs(hotReloadExpectedName)
    }

    @Test
    fun testAddEntity() {
        RestAssured.`when`()["/other-entity/1"].then().statusCode(404)
        TEST.addSourceFile(MyOtherEntity::class.java)
        TEST.addSourceFile(MyOtherTestResource::class.java)
        TEST.modifyResourceFile(
            "import.sql"
        ) { s -> s + s.replace("MyEntity".toRegex(), "MyOtherEntity") }
        RestAssured.`when`()["/other-entity/1"].then().statusCode(200)
            .body(Matchers.`is`("{\"id\":1,\"name\":\"my name\"}"))
    }

    private fun assertBodyIs(expectedBody: String) {
        RestAssured.`when`()["/entity/1"].then().statusCode(200).body(Matchers.`is`(expectedBody))
    }
}