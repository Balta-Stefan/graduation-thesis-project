#! /bin/bash

/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic hourlyConsumptionByConsumer --create --partitions 3
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic totalByCity --create --partitions 3
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic totalConsumption --create --partitions 3
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 --topic input --create --partitions 3