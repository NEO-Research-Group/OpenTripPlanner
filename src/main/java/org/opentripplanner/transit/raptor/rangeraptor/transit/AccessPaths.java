package org.opentripplanner.transit.raptor.rangeraptor.transit;


import static org.opentripplanner.transit.raptor.rangeraptor.transit.AccessEgressFunctions.groupByRound;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.AccessEgressFunctions.removeNoneOptimalPathsForStandardRaptor;

import gnu.trove.map.TIntObjectMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class AccessPaths {
    private final TIntObjectMap<List<RaptorTransfer>> arrivedOnStreetByNumOfRides;
    private final TIntObjectMap<List<RaptorTransfer>> arrivedOnBoardByNumOfRides;

    private AccessPaths(
            TIntObjectMap<List<RaptorTransfer>> arrivedOnStreetByNunOfRides,
            TIntObjectMap<List<RaptorTransfer>> arrivedOnBoardByNunOfRides
    ) {
        this.arrivedOnStreetByNumOfRides = arrivedOnStreetByNumOfRides;
        this.arrivedOnBoardByNumOfRides = arrivedOnBoardByNumOfRides;
    }


    /**
     * The multi-criteria state can handle multiple access/egress paths to a single stop, but the
     * Standard and BestTime states do not. To get a deterministic behaviour we filter the paths
     * and return the paths with the shortest duration for non-multi-criteria search. If two
     * paths have the same duration the first one is picked. Note! If the access/egress paths
     * contains flex as well, then we need to look at mode for arriving at tha stop as well.
     * A Flex arrive-on-board can be used with a transfer even if the time is worse compared with
     * walking.
     * <p>
     * This method is static and package local to enable unit-testing.
     */
    static AccessPaths create(
            Collection<RaptorTransfer> paths,
            RaptorProfile profile
    ) {
        if(!profile.is(RaptorProfile.MULTI_CRITERIA)) {
            paths = removeNoneOptimalPathsForStandardRaptor(paths);
        }
        return new AccessPaths(
                groupByRound(paths, Predicate.not(RaptorTransfer::hasRides)),
                groupByRound(paths, RaptorTransfer::hasRides)
        );
    }

    /**
     * Return the transfer arriving at the stop on-street(walking) grouped by Raptor round.
     * The Raptor round is calculated from the number of rides in the transfer.
     */
    public TIntObjectMap<List<RaptorTransfer>> arrivedOnStreetByNumOfRides() {
        return arrivedOnStreetByNumOfRides;
    }

    /**
     * Return the transfer arriving at the stop on-board a transit(flex) service grouped by
     * Raptor round. The Raptor round is calculated from the number of rides in the transfer.
     */
    public TIntObjectMap<List<RaptorTransfer>> arrivedOnBoardByNumOfRides() {
        return arrivedOnBoardByNumOfRides;
    }

    public int calculateMaxNumberOfRides() {
        return Math.max(
                Arrays.stream(arrivedOnStreetByNumOfRides.keys()).max().orElse(0),
                Arrays.stream(arrivedOnBoardByNumOfRides.keys()).max().orElse(0)
        );
    }

}
