package io.quarkus.it.mongodb.panache.reactive;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;

import io.quarkus.it.mongodb.panache.reactive.person.MockableReactivePersonRepository;
import io.quarkus.it.mongodb.panache.reactive.person.ReactivePersonEntity;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(MongoReplicaSetTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class ReactiveMongodbPanacheMockingTest {

    private static final Duration timeout = Duration.ofSeconds(2);

    @Test
    @Order(1)
    public void testPanacheMocking() {
        PanacheMock.mock(ReactivePersonEntity.class);

        Assertions.assertEquals(0, ReactivePersonEntity.count().await().atMost(timeout));

        Uni<Long> uni23 = Uni.createFrom().item(23L);
        Mockito.when(ReactivePersonEntity.count()).thenReturn(uni23);
        Assertions.assertEquals(uni23, ReactivePersonEntity.count());

        Uni<Long> uni42 = Uni.createFrom().item(42L);
        Mockito.when(ReactivePersonEntity.count()).thenReturn(uni42);
        Assertions.assertEquals(uni42, ReactivePersonEntity.count());

        Mockito.when(ReactivePersonEntity.count()).thenCallRealMethod();
        Assertions.assertEquals(0, ReactivePersonEntity.count().await().atMost(timeout));

        PanacheMock.verify(ReactivePersonEntity.class, Mockito.times(4)).count();

        ReactivePersonEntity p = new ReactivePersonEntity();

        Uni<ReactivePersonEntity> uniP = Uni.createFrom().item(p);
        Mockito.when(ReactivePersonEntity.<ReactivePersonEntity> findById(12L)).thenReturn(uniP);
        Assertions.assertSame(uniP, ReactivePersonEntity.findById(12L));
        Assertions.assertNull(ReactivePersonEntity.findById(42L).await().atMost(timeout));

        ReactivePersonEntity.persist(p);
        Assertions.assertNull(p.id);

        Mockito.when(ReactivePersonEntity.findById(12L)).thenThrow(new WebApplicationException());
        try {
            ReactivePersonEntity.findById(12L);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Uni<List<ReactivePersonEntity>> uniEmptyList = Uni.createFrom().item(Collections.emptyList());
        Mockito.when(ReactivePersonEntity.findOrdered()).thenReturn(uniEmptyList);
        Assertions.assertEquals(uniEmptyList, ReactivePersonEntity.findOrdered());

        PanacheMock.verify(ReactivePersonEntity.class).findOrdered();
        PanacheMock.verify(ReactivePersonEntity.class).persist(Mockito.<Object> any(), Mockito.<Object> any());
        PanacheMock.verify(ReactivePersonEntity.class, Mockito.atLeastOnce()).findById(Mockito.any());
        PanacheMock.verifyNoMoreInteractions(ReactivePersonEntity.class);
    }

    @InjectMock
    MockableReactivePersonRepository mockablePersonRepository;

    @Test
    public void testPanacheRepositoryMocking() throws Throwable {
        Assertions.assertEquals(0, mockablePersonRepository.count().await().atMost(timeout));

        Uni<Long> uni23 = Uni.createFrom().item(23L);
        Mockito.when(mockablePersonRepository.count()).thenReturn(uni23);
        Assertions.assertEquals(uni23, mockablePersonRepository.count());

        Uni<Long> uni42 = Uni.createFrom().item(42L);
        Mockito.when(mockablePersonRepository.count()).thenReturn(uni42);
        Assertions.assertEquals(uni42, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenCallRealMethod();
        Assertions.assertEquals(0, mockablePersonRepository.count().await().atMost(timeout));

        Mockito.verify(mockablePersonRepository, Mockito.times(4)).count();

        ReactivePersonEntity p = new ReactivePersonEntity();
        Uni<ReactivePersonEntity> uniP = Uni.createFrom().item(p);
        Mockito.when(mockablePersonRepository.findById(12L)).thenReturn(uniP);
        Assertions.assertSame(uniP, mockablePersonRepository.findById(12L));
        Assertions.assertNull(mockablePersonRepository.findById(42L).await().atMost(timeout));

        mockablePersonRepository.persist(p);
        Assertions.assertNull(p.id);

        Mockito.when(mockablePersonRepository.findById(12L)).thenThrow(new WebApplicationException());
        try {
            mockablePersonRepository.findById(12L);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Uni<List<ReactivePersonEntity>> uniEmptyList = Uni.createFrom().item(Collections.emptyList());
        Mockito.when(mockablePersonRepository.findOrdered()).thenReturn(uniEmptyList);
        Assertions.assertEquals(uniEmptyList, mockablePersonRepository.findOrdered());

        Mockito.verify(mockablePersonRepository).findOrdered();
        Mockito.verify(mockablePersonRepository, Mockito.atLeastOnce()).findById(Mockito.any());
        Mockito.verify(mockablePersonRepository).persist(Mockito.<ReactivePersonEntity> any());
        Mockito.verifyNoMoreInteractions(mockablePersonRepository);
    }
}
