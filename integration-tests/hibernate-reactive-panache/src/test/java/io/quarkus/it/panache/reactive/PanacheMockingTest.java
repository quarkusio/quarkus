package io.quarkus.it.panache.reactive;

import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class PanacheMockingTest {

    @Test
    @Order(1)
    public void testPanacheMocking() {
        PanacheMock.mock(Person.class);

        Assertions.assertEquals(0, Person.count().await().indefinitely());

        Mockito.when(Person.count()).thenReturn(Uni.createFrom().item(23l));
        Assertions.assertEquals(23, Person.count().await().indefinitely());

        Mockito.when(Person.count()).thenReturn(Uni.createFrom().item(42l));
        Assertions.assertEquals(42, Person.count().await().indefinitely());

        Mockito.when(Person.count()).thenCallRealMethod();
        Assertions.assertEquals(0, Person.count().await().indefinitely());

        PanacheMock.verify(Person.class, Mockito.times(4)).count();

        Person p = new Person();
        Mockito.when(Person.findById(12l)).thenReturn(Uni.createFrom().item(p));
        Assertions.assertSame(p, Person.findById(12l).await().indefinitely());
        Assertions.assertNull(Person.findById(42l).await().indefinitely());

        Person.persist(p).await().indefinitely();
        Assertions.assertNull(p.id);

        Mockito.when(Person.findById(12l)).thenThrow(new WebApplicationException());
        try {
            Person.findById(12l);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Mockito.when(Person.findOrdered()).thenReturn(Uni.createFrom().item(Collections.emptyList()));
        Assertions.assertTrue(Person.findOrdered().await().indefinitely().isEmpty());

        PanacheMock.verify(Person.class).findOrdered();
        PanacheMock.verify(Person.class).persist(Mockito.<Object> any(), Mockito.<Object> any());
        PanacheMock.verify(Person.class, Mockito.atLeastOnce()).findById(Mockito.any());
        PanacheMock.verifyNoMoreInteractions(Person.class);

        Assertions.assertEquals(0, Person.methodWithPrimitiveParams(true, (byte) 0, (short) 0, 0, 2, 2.0f, 2.0, 'c'));
    }

    @Test
    @Order(2)
    public void testPanacheMockingWasCleared() {
        Assertions.assertFalse(PanacheMock.IsMockEnabled);
    }

    @InjectMock
    MockablePersonRepository mockablePersonRepository;

    @Test
    public void testPanacheRepositoryMocking() throws Throwable {
        Assertions.assertEquals(0, mockablePersonRepository.count().await().indefinitely());

        Mockito.when(mockablePersonRepository.count()).thenReturn(Uni.createFrom().item(23l));
        Assertions.assertEquals(23, mockablePersonRepository.count().await().indefinitely());

        Mockito.when(mockablePersonRepository.count()).thenReturn(Uni.createFrom().item(42l));
        Assertions.assertEquals(42, mockablePersonRepository.count().await().indefinitely());

        Mockito.when(mockablePersonRepository.count()).thenCallRealMethod();
        Assertions.assertEquals(0, mockablePersonRepository.count().await().indefinitely());

        Mockito.verify(mockablePersonRepository, Mockito.times(4)).count();

        Person p = new Person();
        Mockito.when(mockablePersonRepository.findById(12l)).thenReturn(Uni.createFrom().item(p));
        Assertions.assertSame(p, mockablePersonRepository.findById(12l).await().indefinitely());
        Assertions.assertNull(mockablePersonRepository.findById(42l).await().indefinitely());

        mockablePersonRepository.persist(p).await().indefinitely();
        Assertions.assertNull(p.id);

        Mockito.when(mockablePersonRepository.findById(12l)).thenThrow(new WebApplicationException());
        try {
            mockablePersonRepository.findById(12l);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Mockito.when(mockablePersonRepository.findOrdered()).thenReturn(Uni.createFrom().item(Collections.emptyList()));
        Assertions.assertTrue(mockablePersonRepository.findOrdered().await().indefinitely().isEmpty());

        Mockito.verify(mockablePersonRepository).findOrdered();
        Mockito.verify(mockablePersonRepository, Mockito.atLeastOnce()).findById(Mockito.any());
        Mockito.verify(mockablePersonRepository).persist(Mockito.<Person> any());
        Mockito.verifyNoMoreInteractions(mockablePersonRepository);
    }

    @Inject
    PersonRepository realPersonRepository;

    @Test
    public void testPanacheRepositoryBridges() {
        // normal method call
        Assertions.assertNull(realPersonRepository.findById(0l).await().indefinitely());
        // bridge call
        Assertions.assertNull(((PanacheRepositoryBase) realPersonRepository).findById(0l).await().indefinitely());
        // normal method call
        Assertions.assertNull(realPersonRepository.findById(0l, LockModeType.NONE).await().indefinitely());
        // bridge call
        Assertions.assertNull(
                ((PanacheRepositoryBase) realPersonRepository).findById(0l, LockModeType.NONE).await().indefinitely());

        // normal method call
        Assertions.assertEquals(false, realPersonRepository.deleteById(0l).await().indefinitely());
        // bridge call
        Assertions.assertEquals(false, ((PanacheRepositoryBase) realPersonRepository).deleteById(0l).await().indefinitely());
    }
}
