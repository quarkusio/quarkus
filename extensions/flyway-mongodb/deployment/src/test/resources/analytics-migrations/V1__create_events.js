db.createCollection("events");
db.events.insertOne({ type: "login", count: 0 });
