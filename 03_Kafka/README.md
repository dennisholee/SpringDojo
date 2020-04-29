
https://docs.confluent.io/current/installation/installing_cp/deb-ubuntu.html#systemd-ubuntu-debian-install

# Setup

## Prepare docker image

docker run -it ubuntu /bin/bash

1. Install dependencies
```
apt install -y openjdk-11-jdk wget gnupg2 software-properties-common vim net-tools iputils-ping
```

2. Install Kafka on Ubuntu
```
wget -qO - https://packages.confluent.io/deb/3.3/archive.key |  apt-key add -
add-apt-repository "deb [arch=amd64] https://packages.confluent.io/deb/3.3 stable main"

apt-get update
apt-get install confluent-community-2.12
```

3. Update zookeeper.properties

```
#/etc/kafka/zookeeper.properties
tickTime=2000
dataDir=/var/lib/zookeeper/
clientPort=2181
initLimit=5
syncLimit=2
server.1=zoo1:2888:3888
server.2=zoo2:2888:3888
server.3=zoo3:2888:3888
autopurge.snapRetainCount=3
autopurge.purgeInterval=24
```

4. Create Docker image
```
docker commit -m "Kafka base image" <image_id> "local/kafka"
```

5. Create Docker network

```
docker create network springdojo
```

## Run Docker instance

1. Spin up the docker instances
```
docker run -it --name zoo1 --hostname zoo1 --net-alias zoo1 --net springdojo \
   -v $(pwd)/zoo1/myid:/var/lib/zookeeper/myid \
   -v $(pwd)/zoo1/server.properties:/etc/kafka/server.properties \
   -v $(pwd)/zoo1/zookeeper.properties:/etc/kafka/zookeeper.properties \
   "local/kafka" /bin/bash

docker run -it --name zoo2 --hostname zoo2 --net-alias zoo2 --net springdojo \
   -v $(pwd)/zoo2/myid:/var/lib/zookeeper/myid \
   -v $(pwd)/zoo2/server.properties:/etc/kafka/server.properties \
   -v $(pwd)/zoo2/zookeeper.properties:/etc/kafka/zookeeper.properties \
   "local/kafka" /bin/bash

docker run -it --name zoo3 --hostname zoo3 --net-alias zoo3 --net springdojo \
   -v $(pwd)/zoo3/myid:/var/lib/zookeeper/myid \
   -v $(pwd)/zoo3/server.properties:/etc/kafka/server.properties \
   -v $(pwd)/zoo3/zookeeper.properties:/etc/kafka/zookeeper.properties \
   "local/kafka" /bin/bash
```

2. Start the services

```
cd /etc/kafka
zookeeper-server-start -daemon zookeeper.properties
kafka-server-start -daemon server.properties
```

# Use cases

Create topic

```
export TOPIC=
export PARTITION_NUMBER=
export REPLICATION_FACTOR=

kafka-topic --bootstrap-server zoo1:9092 --create --topic ${TOPIC} --partitions ${PARTITION_NUMBER} --replication-factor ${REPLICATION_FACTOR}

# Check zookeeper
zookeeper-shell zoo1:2181 ls /brokers/topics/
```

Publish Topic

```
kafka-console-producer --bootstrap-server zoo1:9092 --topic ${TOPIC}
```
