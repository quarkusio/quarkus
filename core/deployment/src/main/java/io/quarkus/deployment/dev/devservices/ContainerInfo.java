package io.quarkus.deployment.dev.devservices;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerInfo {

    private String id;
    private String[] names;
    private String imageName;
    private String status;
    private String[] networks;
    private Map<String, String> labels;
    private ContainerPort[] exposedPorts;

    public ContainerInfo() {
    }

    public ContainerInfo(String id, String[] names, String imageName, String status, String[] networks,
            Map<String, String> labels, ContainerPort[] exposedPorts) {
        this.id = id;
        this.names = names;
        this.imageName = imageName;
        this.status = status;
        this.networks = networks;
        this.labels = labels;
        this.exposedPorts = exposedPorts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortId() {
        return id.substring(0, 12);
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public String formatNames() {
        return String.join(",", getNames());
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String[] getNetworks() {
        return networks;
    }

    public void setNetworks(String[] networks) {
        this.networks = networks;
    }

    public String formatNetworks() {
        return String.join(",", getNetworks());
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public ContainerPort[] getExposedPorts() {
        return exposedPorts;
    }

    public void setExposedPorts(ContainerPort[] exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    public String formatPorts() {
        return Arrays.stream(getExposedPorts())
                .filter(p -> p.getPublicPort() != null)
                .map(c -> c.getIp() + ":" + c.getPublicPort() + "->" + c.getPrivatePort() + "/" + c.getType())
                .collect(Collectors.joining(" ,"));
    }

    public static class ContainerPort {
        private String ip;
        private Integer privatePort;
        private Integer publicPort;
        private String type;

        public ContainerPort() {
        }

        public ContainerPort(String ip, Integer privatePort, Integer publicPort, String type) {
            this.ip = ip;
            this.privatePort = privatePort;
            this.publicPort = publicPort;
            this.type = type;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPrivatePort() {
            return privatePort;
        }

        public void setPrivatePort(Integer privatePort) {
            this.privatePort = privatePort;
        }

        public Integer getPublicPort() {
            return publicPort;
        }

        public void setPublicPort(Integer publicPort) {
            this.publicPort = publicPort;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
