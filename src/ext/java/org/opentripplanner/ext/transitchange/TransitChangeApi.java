package org.opentripplanner.ext.transitchange;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opentripplanner.ext.transitchange.updater.BikeUpdater;
import org.opentripplanner.ext.transitchange.updater.TransitChangeUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/routers/{ignoreRouterId}/transitchange")
@Produces(MediaType.APPLICATION_JSON)
public class TransitChangeApi {

  private static final Logger LOG = LoggerFactory.getLogger(TransitChangeApi.class);

  private final OtpServerRequestContext serverContext;
  private final Graph graph;
  private TransitModel transitModel;

  private TransitChangeUpdater updater;

  public TransitChangeApi(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
    this.graph = serverContext.graph();
  }

  @POST
  @Path("/addGTFS")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response load(@FormDataParam("feed") InputStream feedContent) {
    LOG.info("Loading transit change data");
    try (var output = new ByteArrayOutputStream()) {
      IOUtils.copy(feedContent, output);
      TransitChangeUpdater.addGTFSFeed(
        TransitChangeUpdater.TransitChangeMessage.gtfsFeed(output.toByteArray())
      );
    } catch (IOException e) {
      LOG.error("Error reading feed content", e.getMessage());
    }
    IOUtils.closeQuietly(feedContent);
    return Response.ok().build();
  }

  @POST
  @Path("/addCycleway")
  public Response bikePath() {
    BikeUpdater.addBike("Hola");
    return Response.ok().build();
  }

  public void setTransitModel(TransitModel transitModel) {
    this.transitModel = transitModel;
  }
}
