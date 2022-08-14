#! /bin/bash

/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic hourlyConsumptionByConsumer --delete
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic totalByCity --delete
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic totalConsumption --delete
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic input --delete

/init-scripts/create-simple-aggregator-topics.sh