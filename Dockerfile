FROM java:8-alpine

MAINTAINER Ed Sweeney <ed@onextent.com>

RUN mkdir -p /app

COPY target/scala-2.12/*.jar /app/

WORKDIR /app

# override CMD from your run command, or k8s yaml, or marathon json, etc...
#ENTRYPOINT ["java","-cp", "/app/MqttBridge.jar", "-Xms256m", "-Xmx512m"]
ENTRYPOINT ["java","-cp", "/app/MqttBridge.jar"]

