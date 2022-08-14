FROM docker.io/bitnami/spark:3.3
COPY "./simple-energy-aggregator/target/simple-energy-aggregator-1.0-SNAPSHOT.jar" /opt/bitnami/spark/jars