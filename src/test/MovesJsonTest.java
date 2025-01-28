import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import uk.ac.ed.inf.MovesJson;
import uk.ac.ed.inf.ilp.data.LngLat;


class MovesJsonTest {
    @Test
    void addHoverMove_AddsCorrectHoverMove() {
        List<MovesJson> moves = new ArrayList<>();
        MovesJson initialMove = new MovesJson("ORDER1", -3.186874, 55.944494, 45.0, -3.186874, 55.944494);
        moves.add(initialMove);

        LngLat hoverLocation = new LngLat(-3.186874, 55.944494);
        MovesJson.addHoverMove(moves, hoverLocation, "ORDER1");

        MovesJson hoverMove = moves.get(1);
        assertEquals(999, hoverMove.angle());
        assertEquals("ORDER1", hoverMove.orderNo());
        assertEquals(initialMove.fromLongitude(), hoverMove.fromLongitude());
        assertEquals(initialMove.fromLatitude(), hoverMove.fromLatitude());
        assertEquals(hoverLocation.lng(), hoverMove.toLongitude());
        assertEquals(hoverLocation.lat(), hoverMove.toLatitude());
    }

    @Test
    void addHoverMove_EmptyMoves_ThrowsException() {
        List<MovesJson> moves = new ArrayList<>();
        LngLat location = new LngLat(-3.186874, 55.944494);

        assertThrows(IndexOutOfBoundsException.class,
                () -> MovesJson.addHoverMove(moves, location, "ORDER1"));
    }

    @Test
    void addMoves_AddsCorrectMovesForPath() {
        List<MovesJson> moves = new ArrayList<>();
        List<LngLat> flightPath = List.of(
                new LngLat(-3.186874, 55.944494),
                new LngLat(-3.186874, 55.944594),
                new LngLat(-3.186974, 55.944594)
        );

        MovesJson.addMoves(moves, flightPath, "ORDER1");

        assertEquals(2, moves.size());
        MovesJson firstMove = moves.get(0);
        assertEquals("ORDER1", firstMove.orderNo());
        assertEquals(-3.186874, firstMove.fromLongitude());
        assertEquals(55.944494, firstMove.fromLatitude());
        assertEquals(-3.186874, firstMove.toLongitude());
        assertEquals(55.944594, firstMove.toLatitude());
    }

    @Test
    void addMoves_EmptyFlightPath_AddsNoMoves() {
        List<MovesJson> moves = new ArrayList<>();
        List<LngLat> flightPath = List.of();

        MovesJson.addMoves(moves, flightPath, "ORDER1");

        assertTrue(moves.isEmpty());
    }

    @Test
    void addMoves_SinglePointFlightPath_AddsNoMoves() {
        List<MovesJson> moves = new ArrayList<>();
        List<LngLat> flightPath = List.of(new LngLat(-3.186874, 55.944494));

        MovesJson.addMoves(moves, flightPath, "ORDER1");

        assertTrue(moves.isEmpty());
    }
}