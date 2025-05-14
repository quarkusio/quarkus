package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.CustomDeserialization;
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/simple")
@NonBlocking
public class SimpleJsonResource extends SuperClass<Person> {

    @ServerExceptionMapper
    public Response handleParseException(WebApplicationException e) {
        var cause = e.getCause() == null ? e : e.getCause();
        return Response.status(Response.Status.BAD_REQUEST).entity(cause.getMessage()).build();
    }

    @DisableSecureSerialization
    @GET
    @Path("/person")
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        person.setAddress("10 Downing St");
        person.setBirthDate("November 30, 1874");
        return person;
    }

    @GET
    @Path("/frog")
    public Frog getFrog() {
        var frog = new Frog();
        frog.setPartner(new Frog());
        var pond = new Pond();
        pond.setName("Atlantic Ocean");
        pond.setWaterQuality(Pond.WaterQuality.CLEAR);
        frog.setPonds(List.of(pond));
        return frog;
    }

    @GET
    @Path("/frog-body-parts")
    public FrogBodyParts getFrogBodyParts() {
        return new FrogBodyParts("protruding eyes");
    }

    @GET
    @Path("/interface-dog")
    public SecuredPersonInterface getInterfaceDog() {
        return createDog();
    }

    @GET
    @Path("/abstract-dog")
    public AbstractPet getAbstractDog() {
        return createDog();
    }

    @GET
    @Path("/abstract-named-dog")
    public AbstractNamedPet getAbstractNamedDog() {
        return createDog();
    }

    @GET
    @Path("/dog")
    public Dog getDog() {
        return createDog();
    }

    @POST
    @Path("/dog-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public Dog echoDog(Dog dog) {
        return dog;
    }

    @POST
    @Path("/book-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public Book echoBook(Book book) {
        return book;
    }

    @POST
    @Path("/lombok-book-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public LombokBook echoLombokBook(LombokBook book) {
        return book;
    }

    @POST
    @Path("/record-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public StateRecord echoRecord(StateRecord stateRecord) {
        return stateRecord;
    }

    @POST
    @Path("/empty-ctor-record-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public DogRecord emptyCtorEchoRecord(DogRecord dogRecord) {
        return dogRecord;
    }

    @POST
    @Path("/kotlin-data-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public TokenResponse echoKotlinData(TokenResponse tokenResponse) {
        return tokenResponse;
    }

    @POST
    @Path("/null-map-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public MapWrapper echoNullMap(MapWrapper mapWrapper) {
        return mapWrapper;
    }

    @GET
    @Path("/abstract-cat")
    public AbstractPet getAbstractCat() {
        return createCat();
    }

    @GET
    @Path("/interface-cat")
    public SecuredPersonInterface getInterfaceCat() {
        return createCat();
    }

    @GET
    @Path("/abstract-named-cat")
    public AbstractNamedPet getAbstractNamedCat() {
        return createCat();
    }

    @GET
    @Path("/cat")
    public Cat getCat() {
        return createCat();
    }

    @GET
    @Path("/unsecured-pet")
    public UnsecuredPet getUnsecuredPet() {
        return createUnsecuredPet();
    }

    @GET
    @Path("/abstract-unsecured-pet")
    public AbstractUnsecuredPet getAbstractUnsecuredPet() {
        return createUnsecuredPet();
    }

    @GET
    @Path("/secure-field-on-type-variable")
    public GenericWrapper<Fruit> getWithSecureFieldOnTypeVariable() {
        return new GenericWrapper<>("wrapper", new Fruit("Apple", 1.0f));
    }

    private static UnsecuredPet createUnsecuredPet() {
        var pet = new UnsecuredPet();
        pet.setPublicName("Unknown");
        pet.setVeterinarian(createVeterinarian());
        return pet;
    }

    private static Dog createDog() {
        var dog = new Dog();
        dog.setPublicAge(5);
        dog.setPrivateName("Jack");
        dog.setPublicName("Leo");
        dog.setVeterinarian(createVeterinarian());
        dog.setPublicVaccinated(true);
        return dog;
    }

    private static Cat createCat() {
        var cat = new Cat();
        cat.setPublicName("Garfield");
        cat.setPrivateName("Monday");
        cat.setPrivateAge(4);
        cat.setVeterinarian(createVeterinarian());
        return cat;
    }

    private static Veterinarian createVeterinarian() {
        var vet = new Veterinarian();
        vet.setName("Dolittle");
        vet.setTitle("VMD");
        return vet;
    }

    @CustomSerialization(UnquotedFieldsPersonSerialization.class)
    @GET
    @Path("custom-serialized-person")
    public Person getCustomSerializedPerson() {
        return getPerson();
    }

    @DisableSecureSerialization
    @CustomDeserialization(UnquotedFieldsPersonDeserialization.class)
    @POST
    @Path("custom-deserialized-person")
    public Person echoCustomDeserializedPerson(Person request) {
        return request;
    }

    @GET
    @Path("secure-person")
    public Person getSecurePerson() {
        return getPerson();
    }

    @JsonView(Views.Public.class)
    @GET
    @Path("secure-person-with-public-view")
    public Person getSecurePersonWithPublicView() {
        return getPerson();
    }

    @JsonView(Views.Public.class)
    @GET
    @Path("uni-secure-person-with-public-view")
    public Uni<Person> getUniSecurePersonWithPublicView() {
        return Uni.createFrom().item(getPerson());
    }

    @JsonView(Views.Private.class)
    @GET
    @Path("secure-person-with-private-view")
    public Person getSecurePersonWithPrivateView() {
        return getPerson();
    }

    @GET
    @Path("secure-uni-person")
    public Uni<Person> getSecureUniPerson() {
        return Uni.createFrom().item(getPerson());
    }

    @GET
    @Path("secure-rest-response-person")
    public RestResponse<Person> getSecureRestResponsePerson() {
        return RestResponse.ok(getPerson());
    }

    @GET
    @Path("secure-people")
    public List<Person> getSecurePeople() {
        return Collections.singletonList(getPerson());
    }

    @GET
    @Path("secure-uni-people")
    public Uni<List<Person>> getSecureUniPeople() {
        return Uni.createFrom().item(Collections.singletonList(getPerson()));
    }

    @DisableSecureSerialization
    @POST
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getPerson(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-custom-mt")
    @Produces("application/vnd.quarkus.person-v1+json")
    @Consumes("application/vnd.quarkus.person-v1+json")
    public Person getPersonCustomMediaType(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-custom-mt-response")
    @Produces("application/vnd.quarkus.person-v1+json")
    @Consumes("application/vnd.quarkus.person-v1+json")
    public Response getPersonCustomMediaTypeResponse(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return Response.ok(person).status(201).build();
    }

    @POST
    @Path("/person-custom-mt-response-with-type")
    @Produces("application/vnd.quarkus.person-v1+json")
    @Consumes("application/vnd.quarkus.person-v1+json")
    public Response getPersonCustomMediaTypeResponseWithType(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return Response.ok(person).status(201).header("Content-Type", "application/vnd.quarkus.other-v1+json").build();
    }

    @DisableSecureSerialization
    @POST
    @Path("/people")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Person> getPeople(List<Person> people) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        List<Person> reversed = new ArrayList<>(people.size());
        for (Person person : people) {
            reversed.add(0, person);
        }
        return reversed;
    }

    @CustomDeserialization(UnquotedFieldsPersonDeserialization.class)
    @CustomSerialization(UnquotedFieldsPersonSerialization.class)
    @POST
    @Path("/custom-serialized-people")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Person> getCustomSerializedPeople(List<Person> people) {
        return getPeople(people);
    }

    @POST
    @Path("/strings")
    public List<String> strings(List<String> strings) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return strings;
    }

    @DisableSecureSerialization
    @POST
    @Path("/person-large")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person personTest(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched back to event loop");
        }
        return person;
    }

    @DisableSecureSerialization
    @POST
    @Path("/person-validated")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getValidatedPerson(@Valid Person person) {
        return person;
    }

    @POST
    @Path("/person-invalid-result")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Valid
    public Person getInvalidPersonResult(@Valid Person person) {
        person.setLast(null);
        return person;
    }

    @GET
    @Path("/async-person")
    @Produces(MediaType.APPLICATION_JSON)
    public void getPerson(@Suspended AsyncResponse response) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Person person = new Person();
                person.setFirst("Bob");
                person.setLast("Builder");
                person.setAddress("10 Downing St");
                person.setBirthDate("November 30, 1874");
                response.resume(person);
            }
        }).start();
    }

    @GET
    @Path("/user-without-view")
    public User userWithoutView() {
        return User.testUser();
    }

    @JsonView(Views.Public.class)
    @GET
    @Path("/user-with-public-view")
    public User userWithPublicView() {
        return User.testUser();
    }

    @JsonView(Views.Private.class)
    @GET
    @Path("/user-with-private-view")
    public User userWithPrivateView() {
        return User.testUser();
    }

    @CustomSerialization(UnquotedFieldsPersonSerialization.class)
    @GET
    @Path("/invalid-use-of-custom-serializer")
    public User invalidUseOfCustomSerializer() {
        return User.testUser();
    }

    @GET
    @Path("/multi1")
    public Multi<Person> getMulti1() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        person.setAddress("10 Downing St");
        person.setBirthDate("November 30, 1874");
        return Multi.createFrom().items(person);
    }

    @GET
    @Path("/multi2")
    public Multi<Person> getMulti2() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        person.setAddress("10 Downing St");
        person.setBirthDate("November 30, 1874");
        Person person2 = new Person();
        person2.setFirst("Bob2");
        person2.setLast("Builder2");
        return Multi.createFrom().items(person, person2);
    }

    @GET
    @Path("/multi0")
    public Multi<Person> getMulti0() {
        return Multi.createFrom().empty();
    }

    @POST
    @Path("/genericInput")
    public String genericInputTest(DataItem<Item> item) {
        return item.getContent().getName();
    }

    @GET
    @Path("/interface")
    public ContainerDTO interfaceTest() {
        return new ContainerDTO(NestedInterface.INSTANCE);
    }

    @GET
    @Path("/item")
    public Item getItem() {
        Item item = new Item();
        item.setName("Name");
        item.setEmail("E-mail");
        return item;
    }

    @GET
    @Path("/item-extended")
    public ItemExtended getItemExtended() {
        ItemExtended item = new ItemExtended();
        item.setName("Name");
        item.setEmail("E-mail");
        item.setNameExtended("Name-Extended");
        item.setEmailExtended("E-mail-Extended");
        return item;
    }

    @POST
    @Path("/json-value-public-method")
    @Produces(MediaType.APPLICATION_JSON)
    public ItemJsonValuePublicMethod echoJsonValuePublicMethod(@RestQuery int value) {
        return new ItemJsonValuePublicMethod(value);
    }

    @POST
    @Path("/json-value-public-field")
    @Produces(MediaType.APPLICATION_JSON)
    public ItemJsonValuePublicField echoJsonValuePublicField(@RestQuery int value) {
        return new ItemJsonValuePublicField(value);
    }

    @POST
    @Path("/json-value-private-method")
    @Produces(MediaType.APPLICATION_JSON)
    public ItemJsonValuePrivateMethod echoJsonValuePrivateMethod(@RestQuery int value) {
        return new ItemJsonValuePrivateMethod(value);
    }

    @POST
    @Path("/json-value-private-field")
    @Produces(MediaType.APPLICATION_JSON)
    public ItemJsonValuePrivateField echoJsonValuePrivateField(@RestQuery int value) {
        return new ItemJsonValuePrivateField(value);
    }

    @POST
    @Path("/primitive-types-bean")
    public PrimitiveTypesBean echoPrimitiveTypesBean(PrimitiveTypesBean bean) {
        return bean;
    }

    @POST
    @Path("/primitive-types-record")
    public PrimitiveTypesRecord echoPrimitiveTypesRecord(PrimitiveTypesRecord record) {
        return record;
    }

    public static class UnquotedFieldsPersonSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

        public static final AtomicInteger count = new AtomicInteger();

        public UnquotedFieldsPersonSerialization() {
            count.incrementAndGet();
        }

        @Override
        public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
            if (type instanceof ParameterizedType) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            if (!type.getTypeName().equals(Person.class.getName())) {
                throw new IllegalArgumentException(
                        "Type'" + type.getTypeName() + "' cannot be handled. Only 'Person' type is valid");
            }
            return objectMapper.writer().without(JsonWriteFeature.QUOTE_FIELD_NAMES);
        }
    }

    public static class UnquotedFieldsPersonDeserialization implements BiFunction<ObjectMapper, Type, ObjectReader> {

        public static final AtomicInteger count = new AtomicInteger();

        public UnquotedFieldsPersonDeserialization() {
            count.incrementAndGet();
        }

        @Override
        public ObjectReader apply(ObjectMapper objectMapper, Type type) {
            if (type instanceof ParameterizedType) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            if (!type.getTypeName().equals(Person.class.getName())) {
                throw new IllegalArgumentException(
                        "Type'" + type.getTypeName() + "' cannot be handled. Only 'Person' type is valid");
            }
            return objectMapper.reader().with(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES);
        }
    }

}
