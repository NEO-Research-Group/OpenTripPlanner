package org.opentripplanner.ext.vehiclerentalservicedirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.json.JsonUtils;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdater;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches GBFS endpoints from the micromobility aggregation service located at
 * https://github.com/entur/lahmu, which is an API for aggregating GBFS endpoints.
 */
public class VehicleRentalServiceDirectoryFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(
    VehicleRentalServiceDirectoryFetcher.class
  );
  private static final Duration DEFAULT_FREQUENCY = Duration.ofSeconds(15);

  private final VertexLinker vertexLinker;
  private final VehicleRentalRepository repository;
  private final OtpHttpClient otpHttpClient;

  public VehicleRentalServiceDirectoryFetcher(
    VertexLinker vertexLinker,
    VehicleRentalRepository repository,
    OtpHttpClient otpHttpClient
  ) {
    this.vertexLinker = vertexLinker;
    this.repository = repository;
    this.otpHttpClient = otpHttpClient;
  }

  public static List<GraphUpdater> createUpdatersFromEndpoint(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    VertexLinker vertexLinker,
    VehicleRentalRepository repository
  ) {
    LOG.info("Fetching list of updaters from {}", parameters.getUrl());

    var sources = listSources(parameters);

    if (sources.isEmpty()) {
      return List.of();
    }

    int maxHttpConnections = sources.size();
    var otpHttpClient = new OtpHttpClient(maxHttpConnections);

    return new VehicleRentalServiceDirectoryFetcher(vertexLinker, repository, otpHttpClient)
      .createUpdatersFromEndpoint(parameters, sources);
  }

  public List<GraphUpdater> createUpdatersFromEndpoint(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    JsonNode sources
  ) {
    List<GraphUpdater> updaters = new ArrayList<>();

    for (JsonNode source : sources) {
      Optional<String> network = JsonUtils.asText(source, parameters.getSourceNetworkName());
      Optional<String> updaterUrl = JsonUtils.asText(source, parameters.getSourceUrlName());

      if (network.isEmpty() || updaterUrl.isEmpty()) {
        LOG.warn(
          "Error reading json from {}. Are json tag names configured properly?",
          parameters.getUrl()
        );
      } else {
        createUpdater(parameters, network.get(), updaterUrl.get()).ifPresent(updaters::add);
      }
    }

    LOG.info("{} updaters fetched", updaters.size());

    return updaters;
  }

  private Optional<VehicleRentalUpdater> createUpdater(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    String networkName,
    String updaterUrl
  ) {
    var config = parameters.networkParameters(networkName);

    if (config == null) {
      LOG.info("Network not configured in OTP: {}", networkName);
      return Optional.empty();
    }

    VehicleRentalParameters vehicleRentalParameters = new VehicleRentalParameters(
      "vehicle-rental-service-directory:" + networkName,
      DEFAULT_FREQUENCY,
      new GbfsVehicleRentalDataSourceParameters(
        updaterUrl,
        parameters.getLanguage(),
        // allowKeepingRentedVehicleAtDestination - not part of GBFS, not supported here
        false,
        parameters.getHeaders(),
        networkName,
        config.geofencingZones(),
        // overloadingAllowed - not part of GBFS, not supported here
        false
      )
    );
    LOG.info("Fetched updater info for {} at url {}", networkName, updaterUrl);

    var dataSource = VehicleRentalDataSourceFactory.create(
      vehicleRentalParameters.sourceParameters(),
      otpHttpClient
    );
    return Optional.of( new VehicleRentalUpdater(
      vehicleRentalParameters,
      dataSource,
      vertexLinker,
      repository
    ));
  }

  private static JsonNode listSources(VehicleRentalServiceDirectoryFetcherParameters parameters) {
    JsonNode node;
    URI url = parameters.getUrl();
    try (OtpHttpClient otpHttpClient = new OtpHttpClient()) {
      node = otpHttpClient.getAndMapAsJsonNode(url, Map.of(), new ObjectMapper());
    } catch (OtpHttpClientException e) {
      LOG.warn("Error fetching list of vehicle rental endpoints from {}", url, e);
      return MissingNode.getInstance();
    }
    if (node == null) {
      LOG.warn("Error reading json from {}. Node is null!", url);
      return MissingNode.getInstance();
    }

    String sourcesName = parameters.getSourcesName();
    JsonNode sources = node.get(sourcesName);
    if (sources == null) {
      LOG.warn(
        "Error reading json from {}. No JSON node for sources name '{}' found.",
        url,
        sourcesName
      );
      return MissingNode.getInstance();
    }
    return sources;
  }
}
