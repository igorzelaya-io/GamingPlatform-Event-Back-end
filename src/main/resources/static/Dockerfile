FROM openjdk:11-jre-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} d1gaming-event-backend.jar
ENTRYPOINT ["java", "-jar", "/d1gaming-event-backend.jar"]
EXPOSE 8081