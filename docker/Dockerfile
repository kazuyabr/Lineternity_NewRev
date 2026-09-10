# --- Runtime stage ---
# All compilation and patching is done by Gradle (build/distribution/)
FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache util-linux bash dos2unix mariadb-client

WORKDIR /lineternity
RUN mkdir -p log

COPY . .

RUN dos2unix docker/entrypoint.sh 2>/dev/null; chmod +x docker/entrypoint.sh 2>/dev/null; true

EXPOSE 7777
EXPOSE 2106

ENTRYPOINT ["/lineternity/docker/entrypoint.sh"]
