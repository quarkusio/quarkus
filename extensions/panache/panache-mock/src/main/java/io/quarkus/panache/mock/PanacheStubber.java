package io.quarkus.panache.mock;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

public class PanacheStubber {

    private Stubber stubber;

    public PanacheStubber(Stubber stubber) {
        this.stubber = stubber;
    }

    /**
     * Allows to choose a method when stubbing in doThrow()|doAnswer()|doNothing()|doReturn() style
     * <p>
     * Example:
     * 
     * <pre class="code">
     * <code class="java">
     *   doThrow(new RuntimeException())
     *   .when(mockedList).clear();
     *
     *   //following throws RuntimeException:
     *   mockedList.clear();
     * </code>
     * </pre>
     *
     * Read more about those methods:
     * <p>
     * {@link Mockito#doThrow(Throwable[])}
     * <p>
     * {@link Mockito#doAnswer(Answer)}
     * <p>
     * {@link Mockito#doNothing()}
     * <p>
     * {@link Mockito#doReturn(Object)}
     * <p>
     *
     * See examples in javadoc for {@link Mockito}
     *
     * @param mock The mock
     * @return select method for stubbing
     */
    public <T> T when(Class<T> mock) {
        return stubber.when(PanacheMock.getMock(mock));
    }

}
