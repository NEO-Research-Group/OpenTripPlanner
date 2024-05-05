package org.opentripplanner.ext.transitchange.updater;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.Scope;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BikeUpdater implements GraphUpdater {

  public record OSMWayTagDTO(String key, String value) {}

  public enum BikeUpdateType {
    RESET,
    ADD,
  }

  private record BikeUpdateMessage(BikeUpdateType type, BikeUpdate update) {
    public static BikeUpdateMessage reset() {
      return new BikeUpdateMessage(BikeUpdateType.RESET, null);
    }

    public static BikeUpdateMessage add(BikeUpdate update) {
      return new BikeUpdateMessage(BikeUpdateType.ADD, update);
    }
  }

  public record BikeUpdate(String[] osmNodeIds, boolean bothways, OSMWayTagDTO[] osmTags) {}

  private static final BlockingQueue<BikeUpdateMessage> queue = new LinkedBlockingQueue<>();

  private static final Logger LOG = LoggerFactory.getLogger(TransitChangeUpdater.class);

  private WriteToGraphCallback saveResultOnGraph;
  private final TransitModel transitModel;
  private final VertexLinker linker;
  private final BikeUpdaterParameters parameters;
  private final WayPropertySet wayPropertySet;
  private final Graph graph;
  private DisposableEdgeCollection disposableEdges;

  public BikeUpdater(BikeUpdaterParameters parameters, TransitModel transitModel, Graph graph) {
    this.transitModel = transitModel;
    this.graph = graph;
    this.linker = graph.getLinker();
    this.parameters = parameters;
    this.wayPropertySet = new WayPropertySet();
    this.disposableEdges = new DisposableEdgeCollection(graph, Scope.REALTIME);
    // Using the default tag mapper
    OsmTagMapperSource.DEFAULT.getInstance().populateProperties(wayPropertySet);
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public void run() throws Exception {
    LOG.info("Running bike updater");
    try {
      while (true) {
        BikeUpdateMessage bike = queue.take();

        saveResultOnGraph.execute((graph, transitModel) -> {
          if (bike.type() == BikeUpdateType.RESET) {
            LOG.info("Reseting cycleways");
            LOG.info(String.format("Removing %d edges", disposableEdges.size()));
            disposableEdges.disposeEdges();
          } else {
            LOG.info("Adding bike: {}", bike.update());
            int count = disposableEdges.size();
            addRuta(graph, bike.update());
            LOG.info(String.format("Added %d edges", disposableEdges.size() - count));
          }
        });
      }
    } catch (InterruptedException e) {
      LOG.info("Interrupted: going to finish");
    }
  }

  private VertexLabel computeVertexLabel(String vertexId) {
    String[] parts = vertexId.split("\\|", 2);
    switch (parts[0]) {
      case "OsmNodeLabel":
        return VertexLabel.osm(Long.parseLong(parts[1].substring(9)));
      case "FeedScopedIdLabel":
        return VertexLabel.feedScopedId(FeedScopedId.parse(parts[1]));
      case "OsmNodeOnLevelLabel":
        String[] idLevel = parts[1].substring(9).split("/");
        return VertexLabel.osm(Long.parseLong(idLevel[0]), idLevel[1]);
      case "StringLabel":
        return VertexLabel.string(parts[1]);
      default:
        throw new IllegalArgumentException("Unknown vertex label: " + parts[0]);
    }
  }

  private void addRuta(Graph graph, BikeUpdate bike) {
    String[] ruta = bike.osmNodeIds();
    for (int i = 0; i < ruta.length - 1; i++) {
      if (bike.bothways()) {
        addBothWays(
          graph,
          computeVertexLabel(ruta[i]),
          computeVertexLabel(ruta[i + 1]),
          bike.osmTags()
        );
      } else {
        addBikeEdge(
          graph,
          computeVertexLabel(ruta[i]),
          computeVertexLabel(ruta[i + 1]),
          bike.osmTags()
        );
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
      if (edge.getToVertex().equals(end) && !disposableEdges.contains(edge)) {
        StreetEdge st = (StreetEdge) edge;
        StreetEdgeBuilder builder = new StreetEdgeBuilder(st);
        StreetEdge e = builder
          .withBicycleSafetyFactor((float) wayProperties.getBicycleSafetyFeatures().forward())
          .withWalkSafetyFactor((float) wayProperties.getWalkSafetyFeatures().forward())
          .withPermission(wayProperties.getPermission())
          .withName(st.getName() + " (bike lane)")
          .buildAndConnect();
        disposableEdges.addEdge(e);
        LOG.info("Created edge: " + e.toString());
      }
    }
  }

  @Override
  public String getConfigRef() {
    return parameters.configRef();
  }

  public static void addBike(BikeUpdate update) {
    queue.add(BikeUpdateMessage.add(update));
  }

  public static void resetBike() {
    queue.add(BikeUpdateMessage.reset());
  }
}
