#! /bin/bash

/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 \
			--topic hourlyConsumptionByConsumer \
			--create \
			--partitions 3 \
			--config retention.ms=108000000 \
			--config cleanup.policy=compact,delete \
			--config max.compaction.lag.ms=1200000
			
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 \
			--topic totalByCity \
			--create \
			--partitions 3 \
			--config retention.ms=600000
			
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 \
			--topic totalConsumption \
			--create \
			--partitions 3 \
			--config retention.ms=600000
			
/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server desktop-kafka-1:9092 \
			--topic input \
			--create \
			--partitions 3 \
			--config retention.ms=600000