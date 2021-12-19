package ghs.models;

public class Edge {

    private final int weight;
    private final int toFragmentId;

    public EdgeState state;

    @Override
    public String toString() {
        return "Edge{" +
                "weight=" + weight +
                ", toFragmentId=" + toFragmentId +
                ", state=" + state +
                '}';
    }

    public Edge(int weight, int toFragmentId) {
        this.weight = weight;
        this.toFragmentId = toFragmentId;
        this.state = EdgeState.BASIC;
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
