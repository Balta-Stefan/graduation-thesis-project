FROM docker.io/bitnami/kafka:3.2
COPY --chmod=777 ./scripts/*.sh /init-scripts/