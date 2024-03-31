package org.opentripplanner.ext.transitchange.updater;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetTraversalPermission;
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

  private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  private static final Logger LOG = LoggerFactory.getLogger(TransitChangeUpdater.class);

  private WriteToGraphCallback saveResultOnGraph;
  private final TransitModel transitModel;
  private final VertexLinker linker;
  private final BikeUpdaterParameters parameters;

  public BikeUpdater(
    BikeUpdaterParameters parameters,
    TransitModel transitModel,
    VertexLinker linker
  ) {
    this.transitModel = transitModel;
    this.linker = linker;
    this.parameters = parameters;
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
        String bike = queue.take();
        LOG.info("Adding bike: {}", bike);

        // osm:node:5437890843
        // osm:node:1912833621
        // osm:node:5269915928

        saveResultOnGraph.execute((graph, transitModel) -> {
          addRuta(graph, ruta);
          //addBothWays(graph, VertexLabel.osm(5437890843L), VertexLabel.osm(1912833621L));
          //addBothWays(graph, VertexLabel.osm(1912833621L), VertexLabel.osm(5269915928L));
        });
      }
    } catch (InterruptedException e) {
      LOG.info("Interrupted: goin to finish");
    }
  }

  private void addRuta(Graph graph, long[] ruta) {
    for (int i = 0; i < ruta.length - 1; i++) {
      addBothWays(graph, VertexLabel.osm(ruta[i]), VertexLabel.osm(ruta[i + 1]));
    }
  }

  private void addBothWays(Graph graph, VertexLabel startLabel, VertexLabel endLabel) {
    addBikeEdge(graph, startLabel, endLabel);
    addBikeEdge(graph, endLabel, startLabel);
  }

  private void addBikeEdge(Graph graph, VertexLabel startLabel, VertexLabel endLabel) {
    var start = graph.getVertex(startLabel);
    LOG.info("vertex: " + start.toString() + " outgoing degree: " + start.getDegreeOut());
    var end = graph.getVertex(endLabel);
    for (Edge edge : start.getOutgoing()) {
      if (edge.getToVertex().equals(end)) {
        StreetEdge st = (StreetEdge) edge;
        StreetEdgeBuilder builder = new StreetEdgeBuilder(st);
        StreetEdge e = builder
          .withBicycleSafetyFactor(0.1f)
          .withPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE)
          .withName("Nueva ruta en bici")
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

  public static void addBike(String bike) {
    queue.add(bike);
  }
}
