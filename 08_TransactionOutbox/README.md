# Transaction Outbox Pattern

There are many situations where a combination of database persistence and sending a message needs to be transactional. An example might be an application logic that persists an entity into a repository and then dispatches an event message to a queue to notify event subscribers. One approach is to apply a two-phase commit (2PC) to ensure the two actions are transactional however this may sometimes be unavailable and even so this comes at a cost of performance and scalability.

The sample application requires PostgreSQL and Kafka to be running.

API Endpoint: `http://localhost:8080/claims`


Sample request payload

```
{
    "note": "My note"
}
```

Response payload

```
{
    "id": "ef64ecf5-40bb-4124-ab88-29e65b8d0e85",
    "submissionDate": "2024-06-13",
    "note": "My note"
}
```