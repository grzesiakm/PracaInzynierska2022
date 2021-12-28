package ghs.models;

public class Edge {

    private final int weight;
    private final int toNodeId;

    public EdgeState state;

    @Override
    public String toString() {
        return "to node " + toNodeId +
                " with weight = " + weight;
    }

    public Edge(int weight, int toNodeId) {
        this.weight = weight;
        this.toNodeId = toNodeId;
        this.state = EdgeState.BASIC;
    }

    public int getWeight() {
        return weight;
    }

    public int getToNodeId() {
        return toNodeId;
    }

    public EdgeState getState() {
        return state;
    }

    public void setState(EdgeState state) {
        this.state = state;
    }
}
