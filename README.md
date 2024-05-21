# MOVEN extension of OpenTripPlanner

## Overview

This document focuses on the extension of OpenTripPlanner for accepting new transit modes and new bike lanes. For general information about the usage and functionality of OpenTripPlanner, please, visit the official website at [https://www.opentripplanner.org](https://www.opentripplanner.org), or the official GIT repo at [https://github.com/opentripplanner/OpenTripPlanner](https://github.com/opentripplanner/OpenTripPlanner).

## Docker container

OpenTripPlanner can run in a docker container with all the files nad resources it needs to run. In order to build the container from scratch we need three resources:
1. The source code of the modified version of OpenTripPlanner (in the `src` folder).
2. A map of the region of interest.
3. Public transport information (transit information) in `*.gtfs.zip` files.
4. Config files for OpenTripPlanner.

There are four different `Dockerfile`s prepared to be used during the building process depending on the resources that need to be updated. The file named `Dockerfile` (without any extension) assumes that we want to build a new container based on all the resources mentioned above and we do not have any intermediate result to shorten the building process. This file follow a 4-stage building process and in each stage the following is generating:
1. In the first stage, the `jar` file is generated from the source code. 
2. In the second stage, the so-called "street graph" is generated from a map of region of interest, downloaded from [Geofabrik](http://www.geofabrik.de). As a result a file named `streetGraph.obj` is created (in the intermediate docker container).
3. In the third stage, the complete graph, including transit information (public transportation) is generated. The process takes the file `streetGraph.obj` produced in the previous stage and the public transport information available in the `config` folder. These files should have extension `gtfs.zip`. As the result of this process a file named `graph.obj` is generated with all the information about the public transport and streets (in the intermediate container). 
4. Finally, the docker container serving OpenTripPlanner with the graph generated in the previous stage is built. At this stage the configuration files to tune OpenTripPlanner (with extension `json`) should be available in the `config` folder.

The container can be built with the command (in the same directory where this `README.md` file is):
```
docker build -t moven-otp .
```

The container exposes port 8080/tcp, where the APIs and a simple client is available.

### Shortening the building process with intermediate files

Building the container using the process explained in the previous section can be a slow process (several minutes). This process can be shortened if we provide the intermediate information that the building process needs. In particular:
* If we provide the `jar` file with OpenTripPlanner, we can avoid stage 1. The `jar` file must be in the `target` folder. You can start the building process with `Dockerfile.street`.
* If we provide the `streetGraph.obj` file generated in stage 2 in addition to the `jar` file, we can skip stages 1 and 2. The `jar` file must be in the `target` folder and the `streetGraph.obj` (together with the `*.gtfs.zip` files) must be in the `config` folder. You can start the building process with `Dockerfile.graph`. This shortcut for building the container could be used when new streets are available in the cities. It makes sense to change hte street graph every few week/months.
* If we provide the `graph.obj` file generated in stage 3, in addition to the `jar` file we can skip stages 1, 2 and 3. The `jar` file must be in the `target` folder and the `graph.obj` must be in the `config` folder. You can start the building process with `Dockerfile.final`. This shortcut for building the container is specially interesting to update the transit information, which can change every few weeks/months.

For building the specific `Dockerfile`s you can use the command:
```
docker build -t moven.otp -f {{dockerfile}} .
```

Observe that the intermediate files required above to shorten the building process can be obtained by running the docker build process up to a given target stage (with the `--target` option) and then extracting the generated file from the docker image (commands not shown here).

## Extensions

The extended version of OpenTripPlanner in this repository contains three differente extensions:
1. Two endpoints to add new transit modes and reset the added transits (public transportation modes).
2. Two endpoints to add new bike lanes and remove them from the graph.
3. One vector tile later to query the edges in the graph that can be transformed into bike lanes and the IDs of the nodes at their extremes (to be used in the previous endpoint).

In the following we describe the details of these three functionalities:

### Transit modes

Thre are two endpoints related to the transit modes:
* `/otp/routers/default/transitchange/addGTFS` (POST): this endpoint allows the user to add new transit modes. The new transit mode must be provided in `*.gtfs.zip` format as an attached file. The parameter name for that file is `feed`. A request example for this endpoint is:
```
curl --location 'http://localhost:8080/otp/routers/default/transitchange/addGTFS' \
--form 'feed=@"/home/francis/moven.exp.zip"'
```
* `/otp/routers/default/transitchange/resetGTFS` (POST): this endpoint allows the user to remove all the transit modes previously added using the previous endpoint. This makes it possible to use the same instance of the container for different configurations of the transit modes. A request example for this endpoint is:
```
curl --location --request POST 'http://localhost:8080/otp/routers/default/transitchange/resetGTFS'
```

### Bike lanes

There are two endpoints related to the addition of new bike lanes:
* `/otp/routers/default/transitchange/addCycleway` (POST): this endpoint allows the user to add new bike lanes in the graph. The body of the request is a JSON object with three fields: `osmNodeIds`, `bothways`, and `osmTags`. The lane is specified by providing in `osmNodeIds` a list of IDs of consecutive nodes in the graph. These IDs can be obtained from the vector tile layer described in the next section. The field `bothways` is a Boolean value that indicates if the bike lanes will be one-way (`true`) or two-way (`false`). Finaly, the `osmTags` identify the kind of bike lane. It is possible to provide several key-value pairs identifying the kind of highway. The details about the meaning of these key-value pairs can be found in the official [OpenStreetMap documentation](https://wiki.openstreetmap.org/wiki/Highways). For example, a dedicated bike lane has key-value pair `highway=cycleway`. A request example for this endpoint is:
```
curl --location 'http://localhost:8080/otp/routers/default/transitchange/addCycleway' \
--header 'Content-Type: application/json' \
--data '{
    "osmNodeIds": [
        "OsmNodeLabel|osm:node:418493124",
        "OsmNodeLabel|osm:node:418492935",
        "OsmNodeLabel|osm:node:5882416038",
        "OsmNodeLabel|osm:node:7133340154",
        "OsmNodeLabel|osm:node:418496268",
        "OsmNodeLabel|osm:node:2008332008",
        "OsmNodeLabel|osm:node:3172040868",
        "OsmNodeLabel|osm:node:418496614",
        "OsmNodeLabel|osm:node:7133340155",
        "OsmNodeLabel|osm:node:3172040869",
        "OsmNodeLabel|osm:node:3152128571"
    ],
    "bothways": true,
    "osmTags": [
        {
            "key": "highway",
            "value":"cycleway"
        }
    ]
}'
```


* `/otp/routers/default/transitchange/resetCycleway` (POST): this endpoint allows the user to remove all the bike lanes added using the previous endpoint. This makes it possible to use the same instance of the container for different configurations of the bike lanes. A request example for this endpoint is:
```
curl --location --request POST 'http://localhost:8080/otp/routers/default/transitchange/resetCycleway'
```

### Vector tile layer

The endpoint to add bike lanes require a list of node IDs that OpenTripPlanner recognizes. The information of these IDs can be obtained from the vector tile layer described in this section. This layer uses the binary `pbf` format with the Mapbox Vector Tile representation used in Geographical Information Systems. When a tile is requested, the server sends the edges in that tile that could be transformed into a bike lane. For each edge returns two fields, `origin_osmid` and `destination_osmid`, which contains the node IDs of the tail and the head of the edge. This layer can be access using request in the form: `/otp/routers/default/vectorTiles/osmids/{{z}}/{{x}}/{{y}}.pbf`, where `x`, `y` and `z` are the specification of a tile in the XYZ format (Mapbox Vector Tiles). In particular, `z` is the zoom level and `x` and `y` are the coordinates of the tile at that zoom level. A request example of a tile is:
```
curl --location 'http://localhost:8080/otp/routers/default/vectorTiles/osmids/16/31953/25572.pbf'
```
