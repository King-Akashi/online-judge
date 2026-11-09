FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends time \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /workspace \
    && chown 65534:65534 /workspace

USER 65534:65534
WORKDIR /workspace
