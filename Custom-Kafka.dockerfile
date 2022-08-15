FROM docker.io/bitnami/kafka:3.2
COPY --chmod=777 ./Scripts/*.sh /init-scripts/