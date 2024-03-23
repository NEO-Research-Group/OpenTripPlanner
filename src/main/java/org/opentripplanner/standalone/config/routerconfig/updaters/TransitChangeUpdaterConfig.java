package org.opentripplanner.standalone.config.routerconfig.updaters;

import java.io.File;
import org.opentripplanner.ext.transitchange.updater.TransitChangeUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransitChangeUpdaterConfig {

  public static TransitChangeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new TransitChangeUpdaterParameters(configRef);
  }
}
