package io.quarkus.it.extension.testresources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(value = SomeResource2.class, scope = TestResourceScope.MATCHING_RESOURCES)
@WithTestResource(value = SharedResource.class, scope = TestResourceScope.MATCHING_RESOURCES, initArgs = {
        @ResourceArg(name = "resource.arg", value = "test-two") })
public class WithResourcesPoliciesSecondTest {
    @SomeResource1.Resource1Annotation
    String resource1;
    @SomeResource2.Resource2Annotation
    String resource2;
    @SharedResource.SharedResourceAnnotation
    String sharedResource;

    @Test
    public void checkOnlyResource1started() {
        assertThat(Arrays.asList(resource1, resource2, sharedResource)).isEqualTo(
                Arrays.asList(null, "SomeResource2", "test-two"));
    }
}
