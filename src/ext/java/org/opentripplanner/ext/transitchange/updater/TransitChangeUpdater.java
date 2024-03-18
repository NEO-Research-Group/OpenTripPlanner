package org.opentripplanner.ext.transitchange.updater;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.configure.ConstructApplication;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitChangeUpdater implements GraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(TransitChangeUpdater.class);

  private WriteToGraphCallback saveResultOnGraph;
  private TransitModel transitModel;

  private VertexLinker linker;

  private TransitChangeUpdaterParameters parameters;

  public static BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  public TransitChangeUpdater(
    TransitChangeUpdaterParameters parameters,
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

  @Override
  public void run() throws Exception {
    LOG.info("Running transit change updater");
    while (true) {
      String message = queue.take();
      LOG.info("Received message: {}", message);
      saveResultOnGraph.execute((graph, transitModel) -> {
        LOG.info("Updating graph");
        var time = System.currentTimeMillis();
        List<GraphBuilderModule> builders = new ArrayList<>();
        if (parameters.gtfsFile().exists()) {
          List<GtfsBundle> lista = List.of(new GtfsBundle(parameters.gtfsFile()));
          builders.add(new GtfsModule(lista, transitModel, graph, ServiceDateInterval.unbounded()));
        } else {
          LOG.warn("GTFS file not found {}", parameters.gtfsFile().getAbsolutePath());
        }
        builders.add(new StreetLinkerModule(graph, transitModel, DataImportIssueStore.NOOP, false));
        builders.forEach(GraphBuilderModule::buildGraph);
        ConstructApplication.creatTransitLayerForRaptor(
          transitModel,
          RouterConfig.DEFAULT.transitTuningConfig() // FIXME: we should take the the transit config from the real config, not default
        );
        LOG.info("Graph updated in {} ms", System.currentTimeMillis() - time);
      });
    }
  }

  @Override
  public String getConfigRef() {
    return parameters.configRef();
  }

  public String toString() {
    return "Transit Change updater";
  }
}
