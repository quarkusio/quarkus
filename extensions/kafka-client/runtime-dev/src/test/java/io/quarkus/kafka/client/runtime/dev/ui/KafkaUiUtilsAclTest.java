package io.quarkus.kafka.client.runtime.dev.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaAclInfo;

class KafkaUiUtilsAclTest {

    @Test
    void toAclBindingBuildsAllFields() {
        AclBinding binding = KafkaUiUtils.toAclBinding("User:alice", "10.0.0.1", "READ", "ALLOW", "TOPIC", "quotes",
                "PREFIXED");

        assertThat(binding.entry().principal()).isEqualTo("User:alice");
        assertThat(binding.entry().host()).isEqualTo("10.0.0.1");
        assertThat(binding.entry().operation()).isEqualTo(AclOperation.READ);
        assertThat(binding.entry().permissionType()).isEqualTo(AclPermissionType.ALLOW);
        assertThat(binding.pattern().resourceType()).isEqualTo(ResourceType.TOPIC);
        assertThat(binding.pattern().name()).isEqualTo("quotes");
        assertThat(binding.pattern().patternType()).isEqualTo(PatternType.PREFIXED);
    }

    @Test
    void toAclBindingDefaultsBlankHostToWildcard() {
        assertThat(KafkaUiUtils.toAclBinding("User:alice", null, "READ", "ALLOW", "TOPIC", "quotes", "LITERAL")
                .entry().host()).isEqualTo("*");
        assertThat(KafkaUiUtils.toAclBinding("User:alice", " ", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL")
                .entry().host()).isEqualTo("*");
    }

    @Test
    void toAclBindingIgnoresCase() {
        AclBinding binding = KafkaUiUtils.toAclBinding("User:alice", "*", "read", "deny", "group", "g1", "literal");

        assertThat(binding.entry().operation()).isEqualTo(AclOperation.READ);
        assertThat(binding.entry().permissionType()).isEqualTo(AclPermissionType.DENY);
        assertThat(binding.pattern().resourceType()).isEqualTo(ResourceType.GROUP);
    }

    @Test
    void toAclBindingRejectsMissingPrincipal() {
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding(" ", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("principal");
    }

    @Test
    void toAclBindingRejectsMissingResourceName() {
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "TOPIC", "", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resource name");
    }

    @Test
    void toAclBindingRejectsMissingEnumValues() {
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", null, "ALLOW", "TOPIC", "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operation");
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "", "TOPIC", "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("permission type");
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", null, "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("resource type");
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "TOPIC", "t", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pattern type");
    }

    @Test
    void toAclBindingIgnoresCaseInTurkishLocale() {
        java.util.Locale previous = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
        try {
            assertThat(KafkaUiUtils.toAclBinding("User:alice", "*", "write", "allow", "topic", "t", "literal")
                    .pattern().patternType()).isEqualTo(PatternType.LITERAL);
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    @Test
    void toAclBindingRejectsUnknownValues() {
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "NOPE", "ALLOW", "TOPIC", "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toAclBindingRejectsWildcardValues() {
        // ANY is only valid in filters, never in a real ACL
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "ANY", "ALLOW", "TOPIC", "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ANY", "TOPIC", "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "ANY", "t", "LITERAL"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "TOPIC", "t", "ANY"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteFilterMatchesOnlyTheGivenAcl() {
        AclBinding binding = KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");

        assertThat(binding.toFilter().matches(binding)).as("filter matches its own ACL").isTrue();
        assertThat(binding.toFilter().matches(
                KafkaUiUtils.toAclBinding("User:bob", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL")))
                .as("filter matches another principal").isFalse();
        assertThat(binding.toFilter().matches(
                KafkaUiUtils.toAclBinding("User:alice", "*", "WRITE", "ALLOW", "TOPIC", "quotes", "LITERAL")))
                .as("filter matches another operation").isFalse();
    }

    @Test
    void getAclInfoMapsAllFields() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(null);
        admin.existing.add(KafkaUiUtils.toAclBinding("User:alice", "10.0.0.1", "READ", "DENY", "GROUP", "g1", "PREFIXED"));

        KafkaAclInfo info = utils(admin).getAclInfo();

        assertThat(info.getErrorMessage()).isNull();
        assertThat(info.getClusterId()).isEqualTo("cluster-1");
        assertThat(info.getEntries()).singleElement().satisfies(entry -> {
            assertThat(entry.getPrincipal()).isEqualTo("User:alice");
            assertThat(entry.getHost()).isEqualTo("10.0.0.1");
            assertThat(entry.getOperation()).isEqualTo("READ");
            assertThat(entry.getPerm()).isEqualTo("DENY");
            assertThat(entry.getResourceType()).isEqualTo("GROUP");
            assertThat(entry.getResourceName()).isEqualTo("g1");
            assertThat(entry.getPatternType()).isEqualTo("PREFIXED");
        });
    }

    @Test
    void getAclInfoReportsMissingAuthorizer() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(null);
        admin.describeFailure = new ExecutionException(new SecurityDisabledException("none"));

        KafkaAclInfo info = utils(admin).getAclInfo();

        assertThat(info.getErrorMessage()).contains("security authorizer");
        assertThat(info.getEntries()).isEmpty();
    }

    @Test
    void createAclSendsBindingAndReturnsRefreshedList() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(null);

        KafkaAclInfo info = utils(admin).createAcl("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");

        assertThat(admin.created).singleElement()
                .isEqualTo(KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL"));
        assertThat(info.getErrorMessage()).isNull();
        assertThat(info.getEntries()).singleElement()
                .satisfies(entry -> assertThat(entry.getResourceName()).isEqualTo("quotes"));
    }

    @Test
    void deleteAclSendsBindingAndReturnsRefreshedList() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(null);
        AclBinding binding = KafkaUiUtils.toAclBinding("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");
        admin.existing.add(binding);

        KafkaAclInfo info = utils(admin).deleteAcl("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");

        assertThat(admin.deleted).containsExactly(binding);
        assertThat(info.getErrorMessage()).isNull();
        assertThat(info.getEntries()).isEmpty();
    }

    @Test
    void aclErrorWhenAuthorizerIsMissing() {
        KafkaAclInfo info = KafkaUiUtils.aclError("create", "ALTER",
                new ExecutionException(new SecurityDisabledException("no authorizer")));

        assertThat(info.getErrorMessage()).contains("security authorizer");
    }

    @Test
    void aclErrorWhenNotAuthorized() {
        KafkaAclInfo info = KafkaUiUtils.aclError("delete", "ALTER",
                new ExecutionException(new ClusterAuthorizationException("denied")));

        assertThat(info.getErrorMessage()).contains("delete ACLs").contains("ALTER");
    }

    @Test
    void aclErrorWhenBrokerRejectsRequest() {
        KafkaAclInfo info = KafkaUiUtils.aclError("create", "ALTER",
                new ExecutionException(new InvalidRequestException("bad name")));

        assertThat(info.getErrorMessage()).contains("bad name");
    }

    @Test
    void aclErrorForOtherFailures() {
        KafkaAclInfo info = KafkaUiUtils.aclError("create", "ALTER",
                new ExecutionException(new TimeoutException("timed out")));

        assertThat(info.getErrorMessage()).contains("Failed to create ACL").contains("timed out");
    }

    @Test
    void createAclReportsInvalidInputWithoutCallingBroker() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(null);

        KafkaAclInfo info = utils(admin).createAcl("", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");

        assertThat(info.getErrorMessage()).startsWith("Invalid ACL");
        assertThat(admin.created).isEmpty();
    }

    @Test
    void createAclReportsBrokerFailure() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(new ExecutionException(new SecurityDisabledException("none")));

        KafkaAclInfo info = utils(admin).createAcl("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");

        assertThat(info.getErrorMessage()).contains("security authorizer");
    }

    @Test
    void deleteAclReportsInvalidInputWithoutCallingBroker() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(null);

        KafkaAclInfo info = utils(admin).deleteAcl("User:alice", "*", "READ", "ALLOW", "TOPIC", "", "LITERAL");

        assertThat(info.getErrorMessage()).startsWith("Invalid ACL");
        assertThat(admin.deleted).isEmpty();
    }

    @Test
    void deleteAclReportsBrokerFailure() throws Exception {
        FakeAdminClient admin = new FakeAdminClient(new ExecutionException(new ClusterAuthorizationException("denied")));

        KafkaAclInfo info = utils(admin).deleteAcl("User:alice", "*", "READ", "ALLOW", "TOPIC", "quotes", "LITERAL");

        assertThat(info.getErrorMessage()).contains("delete ACLs");
    }

    private static KafkaUiUtils utils(KafkaAdminClient admin) {
        return new KafkaUiUtils(admin, null, Map.of());
    }

    // In-memory ACL store: records the calls and fails with the given exception, if any
    private static class FakeAdminClient extends KafkaAdminClient {
        final List<AclBinding> existing = new ArrayList<>();
        final List<AclBinding> created = new ArrayList<>();
        final List<AclBinding> deleted = new ArrayList<>();
        ExecutionException describeFailure;
        private final ExecutionException failure;

        FakeAdminClient(ExecutionException failure) {
            this.failure = failure;
        }

        @Override
        public DescribeClusterResult getCluster() {
            // DescribeClusterResult has no public constructor, so it is mocked
            Node node = new Node(1, "localhost", 9092);
            DescribeClusterResult result = mock(DescribeClusterResult.class);
            when(result.controller()).thenReturn(KafkaFuture.completedFuture(node));
            when(result.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(node)));
            when(result.clusterId()).thenReturn(KafkaFuture.completedFuture("cluster-1"));
            when(result.authorizedOperations()).thenReturn(KafkaFuture.completedFuture(Set.of(AclOperation.DESCRIBE)));
            return result;
        }

        @Override
        public Collection<AclBinding> getAclInfo() throws ExecutionException {
            if (describeFailure != null) {
                throw describeFailure;
            }
            return List.copyOf(existing);
        }

        @Override
        public void createAcl(AclBinding aclBinding) throws ExecutionException {
            if (failure != null) {
                throw failure;
            }
            created.add(aclBinding);
            existing.add(aclBinding);
        }

        @Override
        public void deleteAcl(AclBinding aclBinding) throws ExecutionException {
            if (failure != null) {
                throw failure;
            }
            deleted.add(aclBinding);
            existing.remove(aclBinding);
        }
    }
}
