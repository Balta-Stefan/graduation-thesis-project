# Description

![Screenshot](Architecture%20diagram.png)

This is a simplified energy monitoring system created to demonstrate stream and batch processing using Apache Spark.

A data generator application is made using Spring boot.  
The generated data is sent to Apache Kafka.  
Using Spark's structured streaming engine, the data is consumed and aggregated.Results are sent to Kafka.  
Spark performs the following aggregations:
- consumption by every city
- consumption by every client
- hourly consumption by every client

Another Spring boot application is used to ingest data from Kafka to InfluxDB.  
Grafana is used as a dashboard.  

Hourly consumption by every client is periodically saved to MinIO for future analytics, by another Spark application.

All components run within Docker.
