# --- Stage 1: Compile patched LicenseInit.java ---
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

COPY libs/server.jar /build/server.jar
COPY java/ext/mods/security/LicenseInit.java /build/src/LicenseInit.java
COPY java/ext/mods/Config.java /build/src/Config.java
COPY java/ext/mods/commons/config/ExProperties.java /build/src/ExProperties.java
COPY java/ext/mods/commons/lang/StringReplacer.java /build/src/StringReplacer.java

RUN mkdir -p /build/out/ext/mods/security /build/out/ext/mods/commons/config /build/out/ext/mods/commons/lang \
    && javac -d /build/out -cp /build/server.jar /build/src/LicenseInit.java \
    && javac -d /build/out -cp /build/server.jar /build/src/Config.java \
    && javac -d /build/out -cp /build/server.jar /build/src/ExProperties.java \
    && javac -d /build/out -cp /build/server.jar /build/src/StringReplacer.java \
    && jar uf server.jar \
       -C /build/out ext/mods/security/LicenseInit.class \
       -C /build/out ext/mods/Config.class \
       -C /build/out ext/mods/commons/config/ExProperties.class \
       -C /build/out ext/mods/commons/lang/StringReplacer.class \
    && echo "OK: LicenseInit + Config + ExProperties + StringReplacer patched"

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache util-linux bash dos2unix mariadb-client

WORKDIR /l2Brproject
RUN mkdir -p log

COPY . .
COPY --from=builder /build/server.jar /l2Brproject/libs/server.jar

RUN dos2unix docker/entrypoint.sh docker/init-db.sh && chmod +x docker/entrypoint.sh docker/init-db.sh

EXPOSE 7777
EXPOSE 2106

ENTRYPOINT ["/l2Brproject/docker/entrypoint.sh"]
