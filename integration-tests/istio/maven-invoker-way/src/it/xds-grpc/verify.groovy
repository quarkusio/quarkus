import io.dekorate.utils.Serialization
import io.fabric8.kubernetes.api.model.KubernetesList
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.LocalPortForward
import io.fabric8.kubernetes.client.okhttp.OkHttpClientFactory

//Check that file exits
String base = basedir
File kubernetesYml = new File(base, "target/kubernetes/minikube.yml")
assert kubernetesYml.exists()

// Workaround CNFE at shutdown
this.class.getClassLoader().loadClass("io.fabric8.kubernetes.client.KubernetesClientException\$RequestMetadata")

kubernetesYml.withInputStream { stream ->
    //Check that its parse-able
    KubernetesList list = Serialization.unmarshalAsList(stream)
    assert list != null

    Deployment deployment = list.items.find { r -> r.kind == "Deployment" }

    //Check that ti contains a Deployment named after the project
    assert deployment != null
    assert deployment.metadata.name == "xds-grpc"

    try (KubernetesClient client = new KubernetesClientBuilder()
            .withHttpClientFactory(new OkHttpClientFactory())
            .build()) {
        try (LocalPortForward p = client.services().withName("xds-grpc").portForward(8080)) {
            URL url = new URL(String.format("http://localhost:%s/hello", p.localPort));

            int tries = 10;
            String response = null;
            while (response == null && tries > 0) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            String line;
                            StringBuffer buffer = new StringBuffer();
                            while ((line = input.readLine()) != null) {
                                buffer.append(line);
                            }
                            response = buffer.toString();
                        }
                        break;
                    }
                } catch (Exception e) {
                    System.err.print("Error: ")
                    System.err.println(e.getMessage());
                }
                Thread.sleep(6_000); // 6sec
                tries--;
            }
            assert tries > 0
            assert response != null && response == "Hello XDS gRPC"
        }
    }
}