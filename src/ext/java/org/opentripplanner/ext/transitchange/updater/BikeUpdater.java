package org.opentripplanner.ext.transitchange.updater;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BikeUpdater implements GraphUpdater {

  public record OSMWayTagDTO(String key, String value) {}

  public record BikeUpdate(long[] osmNodeIds, boolean bothways, OSMWayTagDTO[] osmTags) {}

  private static final BlockingQueue<BikeUpdate> queue = new LinkedBlockingQueue<>();

  private static final Logger LOG = LoggerFactory.getLogger(TransitChangeUpdater.class);

  private WriteToGraphCallback saveResultOnGraph;
  private final TransitModel transitModel;
  private final VertexLinker linker;
  private final BikeUpdaterParameters parameters;
  private final WayPropertySet wayPropertySet;

  public BikeUpdater(
    BikeUpdaterParameters parameters,
    TransitModel transitModel,
    VertexLinker linker
  ) {
    this.transitModel = transitModel;
    this.linker = linker;
    this.parameters = parameters;
    this.wayPropertySet = new WayPropertySet();
    // Using the default tag mapper
    OsmTagMapperSource.DEFAULT.getInstance().populateProperties(wayPropertySet);
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  private long[] ruta = {
    418493124L,
    418492935L,
    5882416038L,
    7133340154L,
    418496268L,
    2008332008L,
    3172040868L,
    418496614L,
    7133340155L,
    3172040869L,
    3152128571L,
  };

  @Override
  public void run() throws Exception {
    LOG.info("Running bike updater");
    try {
      while (true) {
        BikeUpdate bike = queue.take();
        LOG.info("Adding bike: {}", bike);

        saveResultOnGraph.execute((graph, transitModel) -> {
          addRuta(graph, bike);
        });
      }
    } catch (InterruptedException e) {
      LOG.info("Interrupted: goin to finish");
    }
  }

  private void addRuta(Graph graph, BikeUpdate bike) {
    long[] ruta = bike.osmNodeIds();
    for (int i = 0; i < ruta.length - 1; i++) {
      if (bike.bothways()) {
        addBothWays(graph, VertexLabel.osm(ruta[i]), VertexLabel.osm(ruta[i + 1]), bike.osmTags());
      } else {
        addBikeEdge(graph, VertexLabel.osm(ruta[i]), VertexLabel.osm(ruta[i + 1]), bike.osmTags());
      }
    }
  }

  private void addBothWays(
    Graph graph,
    VertexLabel startLabel,
    VertexLabel endLabel,
    OSMWayTagDTO[] tags
  ) {
    addBikeEdge(graph, startLabel, endLabel, tags);
    addBikeEdge(graph, endLabel, startLabel, tags);
  }

  private void addBikeEdge(
    Graph graph,
    VertexLabel startLabel,
    VertexLabel endLabel,
    OSMWayTagDTO[] tags
  ) {
    var temporalWay = new OSMWay();
    for (OSMWayTagDTO tag : tags) {
      temporalWay.addTag(new OSMTag(tag.key(), tag.value()));
    }
    var wayProperties = wayPropertySet.getDataForWay(temporalWay);
    LOG.info("Way properties: " + wayProperties.toString());

    var start = graph.getVertex(startLabel);
    LOG.info("vertex: " + start.toString() + " outgoing degree: " + start.getDegreeOut());
    var end = graph.getVertex(endLabel);
    for (Edge edge : start.getOutgoing()) {
      if (edge.getToVertex().equals(end)) {
        StreetEdge st = (StreetEdge) edge;
        StreetEdgeBuilder builder = new StreetEdgeBuilder(st);
        StreetEdge e = builder
          .withBicycleSafetyFactor((float) wayProperties.getBicycleSafetyFeatures().forward())
          .withWalkSafetyFactor((float) wayProperties.getWalkSafetyFeatures().forward())
          .withPermission(wayProperties.getPermission())
          .withName(st.getName() + " (bike lane)")
          .buildAndConnect();
        //st.remove();
        LOG.info("Created edge: " + e.toString());
      }
    }
  }

  @Override
  public String getConfigRef() {
    return parameters.configRef();
  }

  public static void addBike(BikeUpdate update) {
    queue.add(update);
  }
}
