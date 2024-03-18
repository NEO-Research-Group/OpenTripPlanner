package org.opentripplanner.ext.transitchange;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
  @Path("/load/{message}")
  public Response load(@PathParam("message") @DefaultValue("") String message) {
    LOG.info("Loading transit change data");
    try {
      TransitChangeUpdater.queue.put(message);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return Response.ok("Hola").build();
  }

  public void setTransitModel(TransitModel transitModel) {
    this.transitModel = transitModel;
  }
}
