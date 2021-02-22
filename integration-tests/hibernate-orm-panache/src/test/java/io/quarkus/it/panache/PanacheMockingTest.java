package io.quarkus.it.panache;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class PanacheMockingTest {

    @Test
    @Order(1)
    public void testPanacheMocking() {
        PanacheMock.mock(Person.class);

        // does not throw (defaults to doNothing)
        Person.voidMethod();

        // make it throw our exception
        Mockito.doThrow(new RuntimeException("Stef")).when(PanacheMock.getMock(Person.class)).voidMethod();
        try {
            Person.voidMethod();
            Assertions.fail();
        } catch (RuntimeException x) {
            Assertions.assertEquals("Stef", x.getMessage());
        }

        // change our exception
        PanacheMock.doThrow(new RuntimeException("Stef2")).when(Person.class).voidMethod();
        try {
            Person.voidMethod();
            Assertions.fail();
        } catch (RuntimeException x) {
            Assertions.assertEquals("Stef2", x.getMessage());
        }

        // back to doNothing
        PanacheMock.doNothing().when(Person.class).voidMethod();
        Person.voidMethod();

        // make it be called
        PanacheMock.doCallRealMethod().when(Person.class).voidMethod();
        try {
            Person.voidMethod();
            Assertions.fail();
        } catch (RuntimeException x) {
            Assertions.assertEquals("void", x.getMessage());
        }

        Assertions.assertEquals(0, Person.count());

        Mockito.when(Person.count()).thenReturn(23l);
        Assertions.assertEquals(23, Person.count());

        Mockito.when(Person.count()).thenReturn(42l);
        Assertions.assertEquals(42, Person.count());

        Mockito.when(Person.count()).thenCallRealMethod();
        Assertions.assertEquals(0, Person.count());

        PanacheMock.verify(Person.class, Mockito.times(4)).count();

        Person p = new Person();
        Mockito.when(Person.findById(12l)).thenReturn(p);
        Assertions.assertSame(p, Person.findById(12l));
        Assertions.assertNull(Person.findById(42l));

        Mockito.when(Person.findById(12l)).thenThrow(new WebApplicationException());
        try {
            Person.findById(12l);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Mockito.when(Person.findOrdered()).thenReturn(Collections.emptyList());
        Assertions.assertTrue(Person.findOrdered().isEmpty());

        PanacheMock.verify(Person.class, Mockito.atLeast(5)).voidMethod();
        PanacheMock.verify(Person.class).findOrdered();
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
        Assertions.assertEquals(0, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenReturn(23l);
        Assertions.assertEquals(23, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenReturn(42l);
        Assertions.assertEquals(42, mockablePersonRepository.count());

        Mockito.when(mockablePersonRepository.count()).thenCallRealMethod();
        Assertions.assertEquals(0, mockablePersonRepository.count());

        Mockito.verify(mockablePersonRepository, Mockito.times(4)).count();

        Person p = new Person();
        Mockito.when(mockablePersonRepository.findById(12l)).thenReturn(p);
        Assertions.assertSame(p, mockablePersonRepository.findById(12l));
        Assertions.assertNull(mockablePersonRepository.findById(42l));

        Mockito.when(mockablePersonRepository.findById(12l)).thenThrow(new WebApplicationException());
        try {
            mockablePersonRepository.findById(12l);
            Assertions.fail();
        } catch (WebApplicationException x) {
        }

        Mockito.when(mockablePersonRepository.findOrdered()).thenReturn(Collections.emptyList());
        Assertions.assertTrue(mockablePersonRepository.findOrdered().isEmpty());

        Mockito.verify(mockablePersonRepository).findOrdered();
        Mockito.verify(mockablePersonRepository, Mockito.atLeastOnce()).findById(Mockito.any());
        Mockito.verifyNoMoreInteractions(mockablePersonRepository);
    }

    @Inject
    PersonRepository realPersonRepository;

    @Test
    public void testPanacheRepositoryBridges() {
        // normal method call
        Assertions.assertNull(realPersonRepository.findById(0l));
        // bridge call
        Assertions.assertNull(((PanacheRepositoryBase) realPersonRepository).findById(0l));
        // normal method call
        Assertions.assertNull(realPersonRepository.findById(0l, LockModeType.NONE));
        // bridge call
        Assertions.assertNull(((PanacheRepositoryBase) realPersonRepository).findById(0l, LockModeType.NONE));

        // normal method call
        Assertions.assertEquals(Optional.empty(), realPersonRepository.findByIdOptional(0l));
        // bridge call
        Assertions.assertEquals(Optional.empty(), ((PanacheRepositoryBase) realPersonRepository).findByIdOptional(0l));
        // normal method call
        Assertions.assertEquals(Optional.empty(), realPersonRepository.findByIdOptional(0l, LockModeType.NONE));
        // bridge call
        Assertions.assertEquals(Optional.empty(),
                ((PanacheRepositoryBase) realPersonRepository).findByIdOptional(0l, LockModeType.NONE));

        // normal method call
        Assertions.assertEquals(false, realPersonRepository.deleteById(0l));
        // bridge call
        Assertions.assertEquals(false, ((PanacheRepositoryBase) realPersonRepository).deleteById(0l));
    }
}
