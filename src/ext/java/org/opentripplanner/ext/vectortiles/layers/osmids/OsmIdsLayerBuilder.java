package org.opentripplanner.ext.vectortiles.layers.osmids;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class OsmIdsLayerBuilder extends LayerBuilder<Vertex> {

  private Graph graph;

  public OsmIdsLayerBuilder(
    Graph graph,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(new OsmIdsMapper(), layerParameters.name(), layerParameters.expansionFactor());
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return graph
      .getStreetIndex()
      .getVerticesForEnvelope(query)
      .stream()
      .map(vertex -> {
        var coordinate = vertex.getCoordinate();
        var point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(vertex);
        return point;
      })
      .collect(Collectors.toList());
  }
}
