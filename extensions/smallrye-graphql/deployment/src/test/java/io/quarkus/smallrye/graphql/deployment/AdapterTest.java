package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.graphql.api.AdaptWith;
import io.smallrye.graphql.api.Adapter;

/**
 * Basic test to make sure adapters is working
 */
public class AdapterTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(AdapterApi.class, Person.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testSourcePost() {
        String personRequest = getPayload("{\n" + "    person {\n" + "       name\n" + "       address {\n"
                + "           city\n" + "       }\n" + "    }\n" + "}");

        RestAssured.given().when().accept(MEDIATYPE_JSON).contentType(MEDIATYPE_JSON).body(personRequest)
                .post("/graphql").then().assertThat().statusCode(200).and()
                .body(CoreMatchers.containsString("\"address\":{\"city\":\"City\"}"));

    }

    @GraphQLApi
    public static class AdapterApi {

        @Query
        public Person getPerson() {
            Address a = new Address();
            a.code = "1234";
            a.addLine("1 Street street");
            a.addLine("City");
            a.addLine("Province");

            Person p = new Person();
            p.setName("Phillip Kruger");
            p.setAddress(a);
            return p;
        }

    }

    public static class Person {

        private String name;
        @AdaptWith(AddressAdapter.class)
        private Address address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        public List<String> lines;
        public String code;

        public void addLine(String line) {
            if (lines == null)
                lines = new LinkedList<>();
            lines.add(line);
        }

    }

    public static class Home {
        public String street;
        public String city;
        public String province;
        public String code;

    }

    public static class AddressAdapter implements Adapter<Address, Home> {

        @Override
        public Home to(Address address) throws Exception {
            Home home = new Home();
            home.code = address.code;
            home.street = address.lines.get(0);
            home.city = address.lines.get(1);
            home.province = address.lines.get(2);
            return home;
        }

        @Override
        public Address from(Home home) throws Exception {
            Address address = new Address();
            address.code = home.code;
            address.addLine(home.street);
            address.addLine(home.city);
            address.addLine(home.province);

            return address;
        }
    }

}
