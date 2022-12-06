package io.quarkus.it.mongodb.panache;

import java.util.Collections;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;

import io.quarkus.it.mongodb.panache.person.MockablePersonRepository;
import io.quarkus.it.mongodb.panache.person.PersonEntity;
import io.quarkus.it.mongodb.panache.person.PersonRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;

@QuarkusTest
@QuarkusTestResource(MongoReplicaSetTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class MongodbPanacheMockingTest {

    @Test
    @Order(1)
    public void testPanacheMocking() {
        PanacheMock.mock(PersonEntity.class);

        Assertions.assertEquals(0, PersonEntity.count());

        Mockito.when(PersonEntity.count()).thenReturn(23L);
        Assertions.assertEquals(23, PersonEntity.count());

        Mockito.when(PersonEntity.count()).thenReturn(42L);
        Assertions.assertEquals(42, PersonEntity.count());

        Mockito.when(PersonEntity.count()).thenCallRealMethod();
        Assertions.assertEquals(0, PersonEntity.count());

        PanacheMock.verify(PersonEntity.class, Mockito.times(4)).count();

        PersonEntity p = new PersonEntity();

        Mockito.when(PersonEntity.findById(12L)).thenReturn(p);
        Assertions.assertSame(p, PersonEntity.findById(12L));
        Assertions.assertNull(PersonEntity.findById(42L));

        PersonEntity.persist(p);
        Assertions.assertNull(p.id);

        Mockito.when(PersonEntity.findById(12L)).thenThrow(new WebApplicationException());
        try {
            PersonEntity.findById(12L);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Mockito.when(PersonEntity.findOrdered()).thenReturn(Collections.emptyList());
        Assertions.assertTrue(PersonEntity.findOrdered().isEmpty());

        PanacheMock.verify(PersonEntity.class).findOrdered();
        PanacheMock.verify(PersonEntity.class).persist(Mockito.<Object> any(), Mockito.<Object> any());
        PanacheMock.verify(PersonEntity.class, Mockito.atLeastOnce()).findById(Mockito.any());
        PanacheMock.verifyNoMoreInteractions(PersonEntity.class);
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
        Assertions.assertEquals(0, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenReturn(23L);
        Assertions.assertEquals(23, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenReturn(42L);
        Assertions.assertEquals(42, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenCallRealMethod();
        Assertions.assertEquals(0, mockablePersonRepository.count());

        Mockito.verify(mockablePersonRepository, Mockito.times(4)).count();

        PersonEntity p = new PersonEntity();
        Mockito.when(mockablePersonRepository.findById(12L)).thenReturn(p);
        Assertions.assertSame(p, mockablePersonRepository.findById(12L));
        Assertions.assertNull(mockablePersonRepository.findById(42L));

        mockablePersonRepository.persist(p);
        Assertions.assertNull(p.id);

        Mockito.when(mockablePersonRepository.findById(12L)).thenThrow(new WebApplicationException());
        try {
            mockablePersonRepository.findById(12L);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Mockito.when(mockablePersonRepository.findOrdered()).thenReturn(Collections.emptyList());
        Assertions.assertTrue(mockablePersonRepository.findOrdered().isEmpty());

        Mockito.verify(mockablePersonRepository).findOrdered();
        Mockito.verify(mockablePersonRepository, Mockito.atLeastOnce()).findById(Mockito.any());
        Mockito.verify(mockablePersonRepository).persist(Mockito.<PersonEntity> any());
        Mockito.verifyNoMoreInteractions(mockablePersonRepository);
    }

    @Inject
    PersonRepository realPersonRepository;

    @Test
    public void testPanacheRepositoryBridges() {
        // normal method call
        Assertions.assertNull(realPersonRepository.findById(0L));
        // bridge call
        Assertions.assertNull(((PanacheMongoRepositoryBase) realPersonRepository).findById(0L));

        // normal method call
        Assertions.assertEquals(Optional.empty(), realPersonRepository.findByIdOptional(0L));
        // bridge call
        Assertions.assertEquals(Optional.empty(), ((PanacheMongoRepositoryBase) realPersonRepository).findByIdOptional(0L));

        // normal method call
        Assertions.assertEquals(false, realPersonRepository.deleteById(0L));
        // bridge call
        Assertions.assertEquals(false, ((PanacheMongoRepositoryBase) realPersonRepository).deleteById(0L));
    }

}
