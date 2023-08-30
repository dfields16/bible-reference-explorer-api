FROM openjdk:17-jdk-slim

VOLUME /tmp
COPY target/*.jar app.jar

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]