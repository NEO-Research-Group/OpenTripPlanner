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

For running the specific `Dockerfile`s you can use the command:
```
docker build -t moven.otp -f {{dockerfile}} .
```

Observe that the intermediate files required above to shorten the building process can be obtained by running the docker build process up to a given target stage (with the `--target` option) and then extracting the generated file from the docker image (commands not shown here).


