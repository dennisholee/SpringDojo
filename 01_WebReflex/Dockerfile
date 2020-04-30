FROM openjdk:14-jdk-alpine

ARG JAR_FILE=build/libs/01_WebReflex.jar

WORKDIR /opt

# copy target/find-links.jar /opt
COPY ${JAR_FILE} app.jar

# java -jar /opt/app.jar
ENTRYPOINT ["java","-jar","app.jar"]