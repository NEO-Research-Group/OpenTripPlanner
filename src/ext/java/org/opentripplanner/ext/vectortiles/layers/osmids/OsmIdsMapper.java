package org.opentripplanner.ext.vectortiles.layers.osmids;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.vertex.Vertex;

public class OsmIdsMapper extends PropertyMapper<Vertex> {

  @Override
  protected Collection<KeyValue> map(Vertex input) {
    return List.of(new KeyValue("id", input.getLabel()));
  }
}
