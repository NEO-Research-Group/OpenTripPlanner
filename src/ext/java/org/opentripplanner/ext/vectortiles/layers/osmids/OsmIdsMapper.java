package org.opentripplanner.ext.vectortiles.layers.osmids;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;

public class OsmIdsMapper extends PropertyMapper<Edge> {

  private String mapLabel(Vertex vertex) {
    String clazzName = vertex.getLabel().getClass().getSimpleName();
    return String.format("%s|%s", clazzName, vertex.getLabel().toString());
  }

  @Override
  protected Collection<KeyValue> map(Edge input) {
    return List.of(
      new KeyValue("origin_osmid", mapLabel(input.getFromVertex())),
      new KeyValue("destination_osmid", mapLabel(input.getToVertex()))
    );
  }
}
