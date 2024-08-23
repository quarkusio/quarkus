package io.quarkus.it.mockbean;

import static org.mockito.Mockito.*;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class SameBeanInstanceTest {

    @Inject
    SameBeanInstance.UnderTest underTest;

    @InjectMock
    SameBeanInstance.I2 i2;

    @InjectMock
    SameBeanInstance.I3 i3;

    @InjectMock
    SameBeanInstance.I4 i4;

    @Test
    public void sample() {
        underTest.method1();

        verify(i2).method2();
        verify(i3).method3();
        verify(i4, never()).method4();
    }
}
