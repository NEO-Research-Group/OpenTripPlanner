# Build OTP from the sources

FROM maven:3.9.6-eclipse-temurin-17-focal as build
COPY . /data
RUN mvn -f /data/pom.xml package -DskipTests

# Build the street graph

FROM eclipse-temurin:17-jre-alpine as street
WORKDIR /data
# Download the Andalucia map
ADD http://download.geofabrik.de/europe/spain/andalucia-latest.osm.pbf /data/otp/
COPY docker-entrypoint.sh /data
COPY --from=build /data/target/*-shaded.jar /data
RUN /data/docker-entrypoint.sh --buildStreet otp


# Build the complete graph, including transit data

FROM eclipse-temurin:17-jre-alpine as graph
WORKDIR /data
# Download the Andalucia map
RUN mkdir -p /data/otp
COPY config/*.gtfs.zip /data/otp
COPY docker-entrypoint.sh /data
COPY --from=build /data/target/*-shaded.jar /data
COPY --from=street /data/otp/streetGraph.obj /data/otp
RUN /data/docker-entrypoint.sh --loadStreet --save otp

# Prepare the final image with the computed graph

FROM eclipse-temurin:17-jre-alpine as final
WORKDIR /data
RUN mkdir -p /data/otp
COPY config/*.json /data/otp
COPY docker-entrypoint.sh /data
COPY --from=build /data/target/*-shaded.jar /data
COPY --from=graph /data/otp/graph.obj /data/otp
EXPOSE 8080/tcp
ENTRYPOINT [ "/data/docker-entrypoint.sh", "--load", "otp" ]
