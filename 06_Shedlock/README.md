# Shedlock Library Study

Shedlock API: [https://github.com/lukas-krecan/ShedLock](https://github.com/lukas-krecan/ShedLock)


## Import project to eclipse

To initialize eclipse environment:
```
mvn eclipse:eclipse
```

Import project to eclipse as maven project.

## Mongo DB

Install MongoDB if required.
Refer to [https://github.com/dennisholee/MongoDBDojo/tree/master/01_Install](https://github.com/dennisholee/MongoDBDojo/tree/master/01_Install).

Create local forward proxy to remote MongoDB is required.

```
ssh -L 27017:localhost:27017 ${ssh_user}@${remote_mongodb_host}
```

