package org.opentripplanner.ext.siri.mapper;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.model.PickDrop.CANCELLED;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;

import java.util.Optional;
import org.opentripplanner.ext.siri.CallWrapper;
import org.opentripplanner.model.PickDrop;
import uk.org.siri.siri20.CallStatusEnumeration;

public class PickDropMapper {

  /**
   * This method maps a CallWrapper to a pick drop type for the stop arrival.
   *
   * The Siri ArrivalBoardingActivity includes less information than the pick drop type, therefore is it only
   * changed if routability has changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param call The incoming call to be mapped
   * @return Mapped PickDrop type, empty if routability is not changed.
   */
  public static Optional<PickDrop> mapDropOffType(CallWrapper call, PickDrop currentValue) {
    if (shouldBeCancelled(currentValue, call.isCancellation(), call.getArrivalStatus())) {
      return Optional.of(CANCELLED);
    }

    var arrivalBoardingActivityEnumeration = call.getArrivalBoardingActivity();
    if (arrivalBoardingActivityEnumeration == null) {
      return Optional.empty();
    }

    return switch (arrivalBoardingActivityEnumeration) {
      case ALIGHTING -> currentValue.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NO_ALIGHTING -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }

  /**
   * This method maps a CallWrapper to a pick drop type for the stop departure.
   *
   * The Siri DepartureBoardingActivity includes less information than the planned data, therefore is it only
   * changed if routability has changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param call The incoming call to be mapped
   * @return Mapped PickDrop type, empty if routability is not changed.
   */
  public static Optional<PickDrop> mapPickUpType(CallWrapper call, PickDrop currentValue) {
    if (shouldBeCancelled(currentValue, call.isCancellation(), call.getDepartureStatus())) {
      return Optional.of(CANCELLED);
    }

    var departureBoardingActivityEnumeration = call.getDepartureBoardingActivity();
    if (departureBoardingActivityEnumeration == null) {
      return Optional.empty();
    }

    return switch (departureBoardingActivityEnumeration) {
      case BOARDING -> currentValue.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NO_BOARDING -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }

  /**
   * Considers if PickDrop should be set to CANCELLED.
   *
   * If the existing PickDrop is non-routable, the value is not changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param isCallCancellation The incoming call cancellation-flag
   * @param callStatus The incoming call arrival/departure status
   * @return
   */
  private static boolean shouldBeCancelled(
    PickDrop currentValue,
    Boolean isCallCancellation,
    CallStatusEnumeration callStatus
  ) {
    if (currentValue.isNotRoutable()) {
      return false;
    }
    return TRUE.equals(isCallCancellation) || callStatus == CallStatusEnumeration.CANCELLED;
  }
}
