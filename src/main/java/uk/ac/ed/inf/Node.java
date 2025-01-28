package uk.ac.ed.inf;
import uk.ac.ed.inf.ilp.data.LngLat;
import java.util.Objects;

/**
 * Node class required to perform A* algorithm
 */
public class Node {
    public LngLat coordinate;
    public double gScore;
    public double hScore;
    public Node parent;

    public boolean explored;

    //constructor to assign default node values
    public Node(LngLat coordinate) {
        this.coordinate = coordinate;
        this.gScore = Double.MAX_VALUE;
        this.hScore = 0.0;
        this.parent = null;
        this.explored = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(coordinate, node.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate);
    }
}