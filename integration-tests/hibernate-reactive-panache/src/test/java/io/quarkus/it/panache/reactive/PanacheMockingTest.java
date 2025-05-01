package io.quarkus.it.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class PanacheMockingTest {

    @SuppressWarnings("static-access")
    @Test
    @RunOnVertxContext
    @Order(1)
    public void testPanacheMocking(UniAsserter asserter) {
        String key = "person";

        asserter.execute(() -> PanacheMock.mock(Person.class));
        asserter.assertEquals(() -> Person.count(), 0l);

        asserter.execute(() -> Mockito.when(Person.count()).thenReturn(Uni.createFrom().item(23l)));
        asserter.assertEquals(() -> Person.count(), 23l);

        asserter.execute(() -> Mockito.when(Person.count()).thenReturn(Uni.createFrom().item(42l)));
        asserter.assertEquals(() -> Person.count(), 42l);

        asserter.execute(() -> Mockito.when(Person.count()).thenCallRealMethod());
        asserter.assertEquals(() -> Person.count(), 0l);

        asserter.execute(() -> {
            // use block lambda here, otherwise mutiny fails with NPE
            PanacheMock.verify(Person.class, Mockito.times(4)).count();
        });

        asserter.execute(() -> {
            Person p = new Person();
            Mockito.when(Person.findById(12l)).thenReturn(Uni.createFrom().item(p));
            asserter.putData(key, p);
        });
        asserter.assertThat(() -> Person.findById(12l), p -> Assertions.assertSame(p, asserter.getData(key)));
        asserter.assertNull(() -> Person.findById(42l));

        asserter.execute(() -> Person.persist(asserter.getData(key)));
        asserter.execute(() -> assertNull(((Person) asserter.getData(key)).id));

        asserter.execute(() -> Mockito.when(Person.findById(12l)).thenThrow(new WebApplicationException()));
        asserter.assertFailedWith(() -> {
            try {
                return Person.findById(12l);
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
        }, t -> assertEquals(WebApplicationException.class, t.getClass()));

        asserter.execute(() -> Mockito.when(Person.findOrdered()).thenReturn(Uni.createFrom().item(Collections.emptyList())));
        asserter.assertThat(() -> Person.findOrdered(), list -> list.isEmpty());

        asserter.execute(() -> {
            PanacheMock.verify(Person.class).findOrdered();
            PanacheMock.verify(Person.class).persist(Mockito.<Object> any(), Mockito.<Object> any());
            PanacheMock.verify(Person.class, Mockito.atLeastOnce()).findById(Mockito.any());
            PanacheMock.verifyNoMoreInteractions(Person.class);
            Assertions.assertEquals(0, Person.methodWithPrimitiveParams(true, (byte) 0, (short) 0, 0, 2, 2.0f, 2.0, 'c'));
        });

        // Execute the asserter within a reactive session
        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @Test
    @Order(2)
    public void testPanacheMockingWasCleared() {
        Assertions.assertFalse(PanacheMock.IsMockEnabled);
    }

    @InjectMock
    MockablePersonRepository mockablePersonRepository;

    @RunOnVertxContext
    @Test
    public void testPanacheRepositoryMocking(UniAsserter asserter) throws Throwable {
        String key = "person";

        asserter.assertEquals(() -> mockablePersonRepository.count(), 0l);

        asserter.execute(() -> Mockito.when(mockablePersonRepository.count()).thenReturn(Uni.createFrom().item(23l)));
        asserter.assertEquals(() -> mockablePersonRepository.count(), 23l);

        asserter.execute(() -> Mockito.when(mockablePersonRepository.count()).thenReturn(Uni.createFrom().item(42l)));
        asserter.assertEquals(() -> mockablePersonRepository.count(), 42l);

        asserter.execute(() -> Mockito.when(mockablePersonRepository.count()).thenCallRealMethod());
        asserter.assertEquals(() -> mockablePersonRepository.count(), 0l);

        asserter.execute(() -> {
            // use block lambda here, otherwise mutiny fails with NPE
            Mockito.verify(mockablePersonRepository, Mockito.times(4)).count();
        });

        asserter.execute(() -> {
            Person p = new Person();
            Mockito.when(mockablePersonRepository.findById(12l)).thenReturn(Uni.createFrom().item(p));
            asserter.putData(key, p);
        });

        asserter.assertThat(() -> mockablePersonRepository.findById(12l), p -> Assertions.assertSame(p, asserter.getData(key)));
        asserter.assertNull(() -> mockablePersonRepository.findById(42l));

        asserter.execute(() -> mockablePersonRepository.persist((Person) asserter.getData(key)));
        asserter.execute(() -> assertNull(((Person) asserter.getData(key)).id));

        asserter.execute(() -> Mockito.when(mockablePersonRepository.findById(12l)).thenThrow(new WebApplicationException()));
        asserter.assertFailedWith(() -> {
            try {
                return mockablePersonRepository.findById(12l);
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
        }, t -> assertEquals(WebApplicationException.class, t.getClass()));

        asserter.execute(() -> Mockito.when(mockablePersonRepository.findOrdered())
                .thenReturn(Uni.createFrom().item(Collections.emptyList())));
        asserter.assertThat(() -> mockablePersonRepository.findOrdered(), list -> list.isEmpty());

        asserter.execute(() -> {
            Mockito.verify(mockablePersonRepository).findOrdered();
            Mockito.verify(mockablePersonRepository, Mockito.atLeastOnce()).findById(Mockito.any());
            Mockito.verify(mockablePersonRepository).persist(Mockito.<Person> any());
            Mockito.verifyNoMoreInteractions(mockablePersonRepository);
        });

        // Execute the asserter within a reactive session
        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @Inject
    PersonRepository realPersonRepository;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RunOnVertxContext
    @Test
    public void testPanacheRepositoryBridges(UniAsserter asserter) {
        // normal method call
        asserter.assertNull(() -> realPersonRepository.findById(0l));
        // bridge call
        asserter.assertNull(() -> ((PanacheRepositoryBase) realPersonRepository).findById(0l));
        // normal method call
        asserter.assertNull(() -> realPersonRepository.findById(0l, LockModeType.NONE));
        // bridge call
        asserter.assertNull(() -> ((PanacheRepositoryBase) realPersonRepository).findById(0l, LockModeType.NONE));

        // normal method call
        asserter.assertFalse(() -> realPersonRepository.deleteById(0l));
        // bridge call
        asserter.assertFalse(() -> ((PanacheRepositoryBase) realPersonRepository).deleteById(0l));

        // Execute the asserter within a reactive session
        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }
}
