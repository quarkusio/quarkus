package io.quarkus.kubernetes.client.deployment;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.singletonList;

import java.util.Base64;

import com.dajudge.kindcontainer.client.config.Cluster;
import com.dajudge.kindcontainer.client.config.ClusterSpec;
import com.dajudge.kindcontainer.client.config.Context;
import com.dajudge.kindcontainer.client.config.ContextSpec;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.User;
import com.dajudge.kindcontainer.client.config.UserSpec;
import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.devservices.common.ContainerAddress;

class APIServerRunningKubeContainer extends RunningKubeContainer {
    private static final String APISERVER = "apiserver";
    private static final String PKI_BASEDIR = "/etc/kubernetes/pki";
    private static final String API_SERVER_CA = PKI_BASEDIR + "/ca.crt";
    private static final String API_SERVER_CERT = PKI_BASEDIR + "/apiserver.crt";
    private static final String API_SERVER_KEY = PKI_BASEDIR + "/apiserver.key";

    APIServerRunningKubeContainer(ContainerAddress containerAddress, InspectContainerResponse containerInfo) {
        super(containerAddress, containerInfo);
    }

    @Override
    protected String kubeConfigFilePath() {
        throw new UnsupportedOperationException("Should not be called");
    }

    @Override
    protected KubeConfig getInternalKubeConfig() {
        final Cluster cluster = new Cluster();
        cluster.setName(APISERVER);
        // cluster URL will need to be set, done in super.getKubeConfig
        cluster.setCluster(new ClusterSpec());
        cluster.getCluster().setCertificateAuthorityData((base64(getFileContentFromContainer(API_SERVER_CA))));
        final User user = new User();
        user.setName(APISERVER);
        user.setUser(new UserSpec());
        user.getUser().setClientKeyData(base64(getFileContentFromContainer(API_SERVER_KEY)));
        user.getUser().setClientCertificateData(base64(getFileContentFromContainer(API_SERVER_CERT)));
        final Context context = new Context();
        context.setName(APISERVER);
        context.setContext(new ContextSpec());
        context.getContext().setCluster(cluster.getName());
        context.getContext().setUser(user.getName());
        final KubeConfig config = new KubeConfig();
        config.setUsers(singletonList(user));
        config.setClusters(singletonList(cluster));
        config.setContexts(singletonList(context));
        config.setCurrentContext(context.getName());
        return config;
    }

    private String base64(final String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(US_ASCII));
    }
}
