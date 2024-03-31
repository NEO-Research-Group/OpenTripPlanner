package org.opentripplanner.standalone.config.routerconfig.updaters;

import org.opentripplanner.ext.transitchange.updater.BikeUpdaterParameters;
import org.opentripplanner.ext.transitchange.updater.TransitChangeUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class BikeUpdaterConfig {

  public static BikeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new BikeUpdaterParameters(configRef);
  }
}
