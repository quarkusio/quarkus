db.users.insertOne({ name: "alice", email: "alice@example.com" });
db.users.createIndex({ email: 1 }, { name: "emailIdx", unique: true });
