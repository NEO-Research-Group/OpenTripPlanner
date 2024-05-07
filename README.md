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

The container exposes port 8080/tcp, where the APIs and a simple client is available.

### Shortening the building process with intermediate files


This `jar` file will be available in the `target` folder with name `otp-{{otp-version}}-MOVEN-{{moven-version}}[-SNAPSHOT]-shaded.jar`.
