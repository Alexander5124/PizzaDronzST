package uk.ac.ed.inf;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Restaurant;


public class LngLatHandler {

    /**
     *
     * @param startPosition- starting co-ordinate
     * @param endPosition - end co-ordinate
     * @return the pythagorean distance between the two co-ordinates
     */
    public double distanceTo(LngLat startPosition, LngLat endPosition){
        double startLng = startPosition.lng();
        double startLat = startPosition.lat();
        double endLng = endPosition.lng();
        double endLat = endPosition.lat();
        return Math.sqrt((Math.pow((startLng - endLng),2)
                + (Math.pow((startLat - endLat),2))));
    }

    /**
     *
     * @param startPosition - current co-ordinate
     * @param otherPosition - co-ordinate to compare to
     * @return - a boolean representing whether the two co-ordinates are close together
     */
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition){
        return distanceTo(startPosition, otherPosition) < 0.00015;
    }

    public boolean isInCentralArea(LngLat point, NamedRegion centralArea) {
        if (centralArea == null) {
            throw new IllegalArgumentException("the named region is null");
        } else if (!centralArea.name().equals("central")) {
            throw new IllegalArgumentException("the named region: " + centralArea.name() + " is not valid - must be: central");
        } else {
            return this.isInRegion(point, centralArea);
        }
    }

    /**
     *
     * @param position - co-ordinates to check
     * @param region - region to compare if the co-ordinates are inside
     * @return -  a boolean verifying if the position is in the region using the ray casting algorithm,
     * by counting the number of interceptions of a ray from the position parameter, if the number is even it means the
     * point is inside the polygon if odd it means the point is outside
     */
    public boolean isInRegion(LngLat position, NamedRegion region) {
        LngLat[] vertices = region.vertices();

        if (vertices == null || vertices.length < 3) {
            return false;  // A polygon must have at least 3 vertices to contain area
        }


        // First check if point is on any vertex or edge
        for (int i = 0; i < vertices.length; i++) {
            // Check if point is on a vertex
            if (isCloseTo(position, vertices[i])) {
                return true;
            }

            // Check if point is on an edge
            LngLat nextVertex = vertices[(i + 1) % vertices.length];
            if (isOnLineSegment(position, vertices[i], nextVertex)) {
                return true;
            }
        }

        // If not on vertex or edge, use ray casting algorithm
        boolean inside = false;
        int j = vertices.length - 1;

        for (int i = 0; i < vertices.length; i++) {
            if (((vertices[i].lat() > position.lat()) != (vertices[j].lat() > position.lat())) &&
                    (position.lng() < (vertices[j].lng() - vertices[i].lng()) * (position.lat() - vertices[i].lat()) /
                            (vertices[j].lat() - vertices[i].lat()) + vertices[i].lng())) {
                inside = !inside;
            }
            j = i;
        }

        return inside;
    }


     /* Helper method to check if a point lies on a line segment
 */
    private boolean isOnLineSegment(LngLat point, LngLat start, LngLat end) {
        // If point is not within the bounding box of the line segment, return false
        if (point.lat() < Math.min(start.lat(), end.lat()) ||
                point.lat() > Math.max(start.lat(), end.lat()) ||
                point.lng() < Math.min(start.lng(), end.lng()) ||
                point.lng() > Math.max(start.lng(), end.lng())) {
            return false;
        }

        // Check if point lies on the line segment using cross product
        double crossProduct = Math.abs((point.lat() - start.lat()) * (end.lng() - start.lng()) -
                (point.lng() - start.lng()) * (end.lat() - start.lat()));

        // Use a small epsilon for floating-point comparison
        return crossProduct < 1e-10;
    }




    /**
     *
     * @param startPosition -
     * @param angle- angle at which the drone will move
     * @return the next position given the angle the drone should move in from its start position
     */

    public LngLat nextPosition(LngLat startPosition, double angle) {
        double distance = SystemConstants.DRONE_MOVE_DISTANCE;

        // Convert angle to radians
        double angleRadians = Math.toRadians(angle);

        // Calculate the change in position using Pythagorean theorem
        double changeInX = distance * Math.cos(angleRadians); // Change in the east-west direction
        double changeInY = distance * Math.sin(angleRadians); // Change in the north-south direction

        // No need to convert to degrees since we are not considering Earth's radius
        // Add the change directly to the starting position
        double newLatitude = startPosition.lat() + changeInY;
        double newLongitude = startPosition.lng() + changeInX;


        return new LngLat(newLongitude, newLatitude);
    }

    /** @param from Starting point
     * @param to   Ending point
     * @return Angle in degrees
     */
    public static double calculateAngle(LngLat from, LngLat to) {
        double deltaLng = to.lng() - from.lng();
        double deltaLat = to.lat() - from.lat();

        // Calculate the angle in radians
        double angleRad = Math.atan2(deltaLat, deltaLng);

        // Convert radians to degrees
        double angleDeg = Math.toDegrees(angleRad);

        // Adjust angle to be in the range [0, 360)
        if (angleDeg < 0) {
            angleDeg += 360.0;
        }

        // Round to the nearest multiple of 22.5
        double angleMultiple = 22.5;
        double roundedAngle = Math.round(angleDeg / angleMultiple) * angleMultiple;

        return roundedAngle;
    }

    public boolean doLineSegmentsIntersect(LngLat p1, LngLat p2, NamedRegion region) {
        LngLat[] regionVertices = region.vertices();

        for (int i = 0; i < regionVertices.length; i++) {
            LngLat q1 = regionVertices[i];
            LngLat q2 = regionVertices[(i + 1) % regionVertices.length];

            if (doLineSegmentsIntersect(p1, p2, q1, q2)) {
                return true;
            }
        }

        return false;
    }

    private boolean doLineSegmentsIntersect(LngLat p1, LngLat p2, LngLat q1, LngLat q2) {
        int o1 = orientation(p1, p2, q1);
        int o2 = orientation(p1, p2, q2);
        int o3 = orientation(q1, q2, p1);
        int o4 = orientation(q1, q2, p2);

        // General case
        if (o1 != o2 && o3 != o4) {
            return true;
        }

        // Special cases
        if (o1 == 0 && onSegment(p1, q1, p2)) return true;
        if (o2 == 0 && onSegment(p1, q2, p2)) return true;
        if (o3 == 0 && onSegment(q1, p1, q2)) return true;
        if (o4 == 0 && onSegment(q1, p2, q2)) return true;

        return false;
    }

    private int orientation(LngLat p, LngLat q, LngLat r) {
        double val = (q.lat() - p.lat()) * (r.lng() - q.lng()) - (q.lng() - p.lng()) * (r.lat() - q.lat());
        if (val == 0) return 0; // Collinear
        return (val > 0) ? 1 : 2; // Clockwise or counterclockwise
    }

    private boolean onSegment(LngLat p, LngLat q, LngLat r) {
        return q.lat() <= Math.max(p.lat(), r.lat()) && q.lat() >= Math.min(p.lat(), r.lat()) &&
                q.lng() <= Math.max(p.lng(), r.lng()) && q.lng() >= Math.min(p.lng(), r.lng());
    }




}
