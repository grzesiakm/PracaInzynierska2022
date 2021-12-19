package ghs.models;

public class Edge {

    private final int weight;
    private final int toFragmentId;
    private final int edgeId;

    public EdgeState state;

    @Override
    public String toString() {
        return "Edge{" +
                "weight=" + weight +
                ", toFragmentId=" + toFragmentId +
                ", state=" + state +
                '}';
    }
//Todo dodaj edgeId
    public Edge(int weight, int toFragmentId) {
        this.weight = weight;
        this.toFragmentId = toFragmentId;
        this.edgeId = weight;
        this.state = EdgeState.BASIC;
    }

    public int getEdgeId() {
        return edgeId;
    }

    public int getWeight() {
        return weight;
    }

    public int getToFragmentId() {
        return toFragmentId;
    }

    public EdgeState getState() {
        return state;
    }

    public void setState(EdgeState state) {
        this.state = state;
    }
}
