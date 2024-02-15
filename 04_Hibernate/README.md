
# Spring PostGreSQL Demo

## Overview

The application covers the following sample:

1. Using PostGreSQL as a message broker
2. Enabling Hibernate level 2 cache using Redis

## Guidelines

##### PostGreSQL 

Start an instance of PostGreSQL container

```
docker run --name some-postgres -p 5432:5432  -e POSTGRES_PASSWORD={PASSWORD} -d postgres
```

Connect to PostGreSQL's container via the command line

```
docker exec -it some-postgres /bin/bash
```

Connect to PostGreSQL instance

```
psql -u postgres
```

##### Redis

Start an instance of Redis container

```
docker run --name some-redis -p 6379:6379 -d redis
```

Connect to Redis container via the command line

```
docker exec -it some-redis redis-cli
```

Validate Redis connectivity

```
127.0.0.1:6379> ping
PONG
```

##### Start application

```
mvn spring-boot:run
```

### Call API

##### Create Message

Create message by invoking end-point `POST /notify` as follows

```
curl -X POST http://localhost:8080/notify -H "Content-Type: application/json" -d '{"message":"foobar"}'
```

The following Hibernate statistics indicates data is cached in level 2 cache.

```
Hibernate: 
    select
        n1_0.id,
        n1_0.create_date_time,
        n1_0.message,
        n1_0.modify_date_time 
    from
        notifications n1_0 
    where
        n1_0.id=?
Hibernate: 
    insert 
    into
        notifications
        (create_date_time, message, modify_date_time, id) 
    values
        (?, ?, ?, ?)
2024-02-15T16:42:05.922+08:00  INFO 99049 --- [ctor-http-nio-3] i.StatisticalLoggingSessionEventListener : Session Metrics {
    23125 nanoseconds spent acquiring 1 JDBC connections;
    0 nanoseconds spent releasing 0 JDBC connections;
    482750 nanoseconds spent preparing 2 JDBC statements;
    11015833 nanoseconds spent executing 2 JDBC statements;
    0 nanoseconds spent executing 0 JDBC batches;
    61187875 nanoseconds spent performing 4 L2C puts;
    0 nanoseconds spent performing 0 L2C hits;
    62934876 nanoseconds spent performing 2 L2C misses;
    29177416 nanoseconds spent executing 1 flushes (flushing a total of 1 entities and 0 collections);
    0 nanoseconds spent executing 0 partial-flushes (flushing a total of 0 entities and 0 collections)
```

The following shows the keys in Redis

```
127.0.0.1:6379> keys *
1) "io.forest.hibernate.adapter.repository.sql.model.Notification"
2) "default-update-timestamps-region"
3) "redisson-hibernate-timestamp"

127.0.0.1:6379> hgetall io.forest.hibernate.adapter.repository.sql.model.Notification
1) "\x01\x00org.hibernate.cache.internal.BasicCacheKeyImplementatio\xee\xbeio.forest.hibernate.adapter.repository.sql.model.Notification\xf5\xe9\x86\xed\n\x03\xa57f466511-a9a5-4f78-b3e4-7811922686d6"
2) "\x00\x00\x00\x00\x00\x00\x00\x00\xdd\x00\x00\x00\x00\x00\x00\x00\x01\x00org.hibernate.cache.spi.support.AbstractReadWriteAccess$Ite\xed\x80\xc0\xea\x9e\xdc\xaf\xed\x18\x01\x01org.hibernate.cache.spi.entry.StandardCacheEntryImp\xec\x01\x02[Ljava.io.Serializable\xbb\x04\x00\x03tes\xf4\x00\xbeio.forest.hibernate.adapter.repository.sql.model.Notification\x00\x00"

```

##### Find All Messages

Find all messages by invoking end-point `GET /notify` as follows

```
curl http://localhost:8080/notify
[{"id":"7f466511-a9a5-4f78-b3e4-7811922686d6","createDateTime":null,"modifyDateTime":null,"message":"foobar"}]
```

The following Hibernate statistics indicates data is fetched from database.

```
Hibernate: 
    select
        n1_0.id,
        n1_0.create_date_time,
        n1_0.message,
        n1_0.modify_date_time 
    from
        notifications n1_0
2024-02-15T16:57:34.088+08:00  INFO 99049 --- [ctor-http-nio-4] i.StatisticalLoggingSessionEventListener : Session Metrics {
    17002875 nanoseconds spent acquiring 1 JDBC connections;
    0 nanoseconds spent releasing 0 JDBC connections;
    793416 nanoseconds spent preparing 1 JDBC statements;
    15929667 nanoseconds spent executing 1 JDBC statements;
    0 nanoseconds spent executing 0 JDBC batches;
    11517125 nanoseconds spent performing 1 L2C puts;
    0 nanoseconds spent performing 0 L2C hits;
    0 nanoseconds spent performing 0 L2C misses;
    0 nanoseconds spent executing 0 flushes (flushing a total of 0 entities and 0 collections);
    4250 nanoseconds spent executing 1 partial-flushes (flushing a total of 0 entities and 0 collections)
}
```

##### Find By ID Message

Get individual message by invoking end-point `GET /notify/{id}` as follows

```
curl --location 'localhost:8080/notify/7f466511-a9a5-4f78-b3e4-7811922686d6'
{"id":"7f466511-a9a5-4f78-b3e4-7811922686d6","createDateTime":null,"modifyDateTime":null,"message":"foobar"}
```

The following Hibernate statistics indicates data is fetched from level 2 cache.

```
2024-02-15T17:00:06.567+08:00  INFO 99049 --- [ctor-http-nio-5] i.StatisticalLoggingSessionEventListener : Session Metrics {
    6217250 nanoseconds spent acquiring 1 JDBC connections;
    0 nanoseconds spent releasing 0 JDBC connections;
    0 nanoseconds spent preparing 0 JDBC statements;
    0 nanoseconds spent executing 0 JDBC statements;
    0 nanoseconds spent executing 0 JDBC batches;
    0 nanoseconds spent performing 0 L2C puts;
    15201875 nanoseconds spent performing 1 L2C hits;
    0 nanoseconds spent performing 0 L2C misses;
    0 nanoseconds spent executing 0 flushes (flushing a total of 0 entities and 0 collections);
    0 nanoseconds spent executing 0 partial-flushes (flushing a total of 0 entities and 0 collections)
```



At the PostGreSQL prompt, subscribe to channel

Note: "postgres=#" is the console prompt.


```console
postgres=# listen demochannel;
LISTEN
Asynchronous notification "demochannel" with payload "NotificationDto(id=0dd36298-e77d-43f9-b4d8-837710c91b0d, createDateTime=null, modifyDateTime=null, message=postman)" received from server process with PID 20725.
```

To get details of the table and its schema

```
postgres=# \d
 public | notifications | table | postgres

postgres=# \d notifications;
 create_date_time | timestamp(6) without time zone |           |          | 
 modify_date_time | timestamp(6) without time zone |           |          | 
 id               | character varying(255)         |           | not null | 
 message          | character varying(255)         |           |          | 
 ```
 
## References
* [PostGreSQL Notify](https://www.postgresql.org/docs/current/sql-notify.html)
* [Using PostgreSQL as a Message Broker](https://www.baeldung.com/spring-postgresql-message-broker)
* [Redis based Hibernate Cache Implementation](https://github.com/redisson/redisson/blob/master/redisson-hibernate/README.md)
* [Scaling Spring Boot with Hibernate 2nd Level Cache on Redis](https://medium.com/@shahto/scaling-spring-boot-with-hibernate-2nd-level-cache-on-redis-54d588fc8b06#:~:text=Level%202%20Cache%3A%20is%20a,is%20therefore%20disabled%20by%20default.)
