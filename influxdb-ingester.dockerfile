FROM openjdk:11-jre-slim
COPY InfluxDB-ingester/target/*.jar aplikacija.jar
RUN useradd -u 1234 non-root
USER non-root
ENTRYPOINT [ "java", "-jar", "aplikacija.jar" ]