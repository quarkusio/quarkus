package io.quarkus.it.mongodb.discriminator.jsonb;

import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import io.quarkus.it.mongodb.discriminator.Car;
import io.quarkus.it.mongodb.discriminator.Moto;
import io.quarkus.it.mongodb.discriminator.Vehicle;

public class VehicleDeserializer implements JsonbDeserializer<Vehicle> {
    @Override
    public Vehicle deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        JsonObject json = parser.getObject();
        String type = json.getString("type");
        switch (type) {
            case "CAR":
                return new Car(type, json.getString("name"), json.getInt("seatNumber"));
            case "MOTO":
                return new Moto(type, json.getString("name"), json.getBoolean("sideCar"));
            default:
                throw new RuntimeException("Type " + type + " not managed");
        }
    }
}
