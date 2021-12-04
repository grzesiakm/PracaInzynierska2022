package ghs.messaging;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Actor actor2 = new Actor(50052);
        Actor actor3 = new Actor(50053);

        actor2.initialize();
        actor3.initialize();

        actor2.sendMsg("ping", 50053);

        Thread.sleep(15000);

        actor2.finish();
        actor3.finish();

    }
}
