FROM openjdk:19-jdk-alpine
VOLUME /tmp
EXPOSE 8080
ARG JAR_FILE=SearchEngine-1.0-SNAPSHOT.jar
ADD ${JAR_FILE} SearchEngine-1.0-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/SearchEngine-1.0-SNAPSHOT.jar"]