FROM amazoncorretto:17.0.7-alpine

VOLUME /tmp
COPY target/*.jar app.jar

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]