### Usage

- Add this extension for MQTT messaging using SmallRye Reactive Messaging.
- Use `@Incoming("channel")` to consume MQTT messages and `@Outgoing("channel")` to publish.
- Configure the MQTT broker with `mp.messaging.incoming.<channel>.connector=smallrye-mqtt`.

### Pattern

```java
@ApplicationScoped
public class MqttProcessor {
    @Incoming("sensor-data")
    public void process(byte[] payload) { ... }

    @Outgoing("commands")
    public Multi<String> generate() { ... }
}
```

### Configuration

- Set broker: `mp.messaging.incoming.sensor-data.host=localhost` and `mp.messaging.incoming.sensor-data.port=1883`.
- Set topic: `mp.messaging.incoming.sensor-data.topic=sensors/temperature`.

### Testing

- Use `@QuarkusTest` with an MQTT test container or embedded broker.
- Use `InMemoryConnector` from SmallRye for unit testing without a broker.

### Common Pitfalls

- MQTT topics use `/` as separator (not `.` like Kafka topics).
- MQTT QoS levels (0, 1, 2) affect delivery guarantees — configure appropriately.
- For Kafka messaging, use `quarkus-smallrye-reactive-messaging-kafka` instead.
