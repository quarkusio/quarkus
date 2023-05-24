package io.quarkus.it.envers;

public class Message2 {

    private String data;

    public Message2() {
    }

    public Message2(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
