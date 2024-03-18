package org.opentripplanner.ext.transitchange.updater;

import java.io.File;

public record TransitChangeUpdaterParameters(String configRef, File gtfsFile) {}
