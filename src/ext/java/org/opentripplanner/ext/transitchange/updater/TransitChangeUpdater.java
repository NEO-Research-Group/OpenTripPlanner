package org.opentripplanner.ext.transitchange.updater;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
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

  public enum TransitChangeMessageType {
    GTFS_FEED,
    FINISH,
  }

  public record TransitChangeMessage(byte[] data, TransitChangeMessageType type) {
    public static TransitChangeMessage finish() {
      return new TransitChangeMessage(new byte[0], TransitChangeMessageType.FINISH);
    }

    public static TransitChangeMessage gtfsFeed(byte[] data) {
      return new TransitChangeMessage(data, TransitChangeMessageType.GTFS_FEED);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(TransitChangeUpdater.class);

  private WriteToGraphCallback saveResultOnGraph;
  private TransitModel transitModel;

  private VertexLinker linker;

  private TransitChangeUpdaterParameters parameters;

  private static BlockingQueue<TransitChangeMessage> queue = new LinkedBlockingQueue<>();

  private List<ZipFileDataSource> gtfsFeeds = new ArrayList<>();

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

  public static void addGTFSFeed(TransitChangeMessage message) {
    queue.add(message);
  }

  private static void finish() {
    queue.add(TransitChangeMessage.finish());
  }

  @Override
  public void run() throws Exception {
    LOG.info("Running transit change updater");
    boolean isRunning = true;

    while (isRunning) {
      try {
        var message = queue.take();
        LOG.info("Received message of type: {}", message.type());
        if (message.type() == TransitChangeMessageType.FINISH) {
          isRunning = false;
          continue;
        } else if (message.type() != TransitChangeMessageType.GTFS_FEED) {
          LOG.warn("Unknown message type {}", message.type());
          continue;
        }

        List<TransitChangeMessage> messages = new ArrayList<>();
        while (message != null && message.type() == TransitChangeMessageType.GTFS_FEED) {
          messages.add(message);
          message = queue.peek();
          if (message != null && message.type() == TransitChangeMessageType.GTFS_FEED) {
            queue.poll();
          }
        }

        var bundles = messages
          .stream()
          .map(this::dataSourceFromMessage)
          .filter(d -> d != null)
          .peek(gtfsFeeds::add)
          .map(GtfsBundle::new)
          .collect(Collectors.toList());

        saveResultOnGraph.execute((graph, transitModel) -> {
          LOG.info("Updating graph");
          var time = System.currentTimeMillis();
          List<GraphBuilderModule> builders = new ArrayList<>();
          builders.add(
            new GtfsModule(bundles, transitModel, graph, ServiceDateInterval.unbounded())
          );
          builders.add(
            new StreetLinkerModule(graph, transitModel, DataImportIssueStore.NOOP, false)
          );
          builders.forEach(GraphBuilderModule::buildGraph);
          ConstructApplication.creatTransitLayerForRaptor(
            transitModel,
            RouterConfig.DEFAULT.transitTuningConfig() // FIXME: we should take the the transit config from the real config, not default
          );
          LOG.info("Graph updated in {} ms", System.currentTimeMillis() - time);
        });
      } catch (InterruptedException e) {
        LOG.info(this + " interrupted: going to stop");
        isRunning = false;
      }
    }
    LOG.info("Finishing " + this);
  }

  private ZipFileDataSource dataSourceFromMessage(TransitChangeMessage message) {
    try {
      Path path = Files.createTempFile("transitchange", ".gtfs.zip");
      Files.write(path, message.data());
      return new ZipFileDataSource(path.toFile(), FileType.GTFS);
    } catch (IOException e) {
      LOG.error("Error writing GTFS feed", e.getMessage());
      return null;
    }
  }

  @Override
  public String getConfigRef() {
    return parameters.configRef();
  }

  public String toString() {
    return "Transit Change updater";
  }

  @Override
  public void teardown() {
    LOG.info("Stopping transit change updater");
    deleteGTFSFeeds();
    finish();
    LOG.info("Finish message sent");
  }

  private void deleteGTFSFeeds() {
    gtfsFeeds.stream().map(zip -> zip.uri()).map(Path::of).map(Path::toFile).forEach(File::delete);
  }
}
