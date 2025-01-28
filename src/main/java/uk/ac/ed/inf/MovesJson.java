package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.ed.inf.ilp.data.LngLat;
import java.util.List;
import static uk.ac.ed.inf.LngLatHandler.calculateAngle;


/**
 * A record to represent a single unit of the MovesJson Json output file
 */
public record MovesJson(
        @JsonProperty("orderNo") String orderNo,
        @JsonProperty("fromLongitude") double fromLongitude,
        @JsonProperty("fromLatitude") double fromLatitude,
        @JsonProperty("angle") double angle,
        @JsonProperty("toLongitude") double toLongitude,
        @JsonProperty("toLatitude") double toLatitude
) {


    /**
     * A method to enable the addition of a hover move to a pre-existing array of moves
     *
     * @param moves    - pre-existing array of Moves
     * @param location - current location that the hover will occur at
     * @param orderNo  - order number of the order
     */
    public static void addHoverMove(List<MovesJson> moves, LngLat location, String orderNo) {
        MovesJson hoverMove = new MovesJson(orderNo, moves.get(moves.size() - 1).fromLongitude, moves.get(moves.size() - 1).fromLatitude,
                999, location.lng(), location.lat());
        moves.add(hoverMove);
    }


    /**
     * A method to add a flight path(in MovesJson format) to a pre-existing array of previous moves
     *
     * @param moves               - pre-existing array of moves
     * @param mostRecentFlightPath - current flight path calculated to be added
     * @param orderNumber         - order number of the order
     */
    public static void addMoves(List<MovesJson> moves, List<LngLat> mostRecentFlightPath, String orderNumber) {
        for (int i = 0; i < mostRecentFlightPath.size() - 1; i++) {
            LngLat fromLngLat = mostRecentFlightPath.get(i);
            LngLat toLngLat = mostRecentFlightPath.get(i + 1);

            MovesJson move = new MovesJson(orderNumber, fromLngLat.lng(), fromLngLat.lat(), calculateAngle(fromLngLat, toLngLat),
                    toLngLat.lng(), toLngLat.lat());
            moves.add(move);
        }
    }
}