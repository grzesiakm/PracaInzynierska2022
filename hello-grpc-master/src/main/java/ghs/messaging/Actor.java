package ghs.messaging;

import generated.NodeMessage;
import generated.MessageHandlerGrpc;
import generated.Ok;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Actor extends MessageHandlerGrpc.MessageHandlerImplBase {

    private static final Logger logger = Logger.getLogger(Actor.class.getName());
    private final Queue<NodeMessage> queue;

    public int getPort() {
        return port;
    }

    private final int port;
    private boolean alive;
    private Server server;

    public Actor(int port) {
        this.queue = new LinkedList<>();
        this.port = port;
    }

    public void initialize() {
        this.alive = true;
        new Thread(this::handleIncomingMessages).start();
        new Thread(this::processMessageQueue).start();
    }

    public void finish() {
        server.shutdown();
        this.alive = false;
    }

    @Override
    public void handleMessage(NodeMessage nodeMessage, StreamObserver<Ok> okObserver) {
        enqueue(nodeMessage);
        okObserver.onNext(Ok.newBuilder().build());
        okObserver.onCompleted();
    }

    private void handleIncomingMessages() {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(this)
                    .build()
                    .start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    if (server != null) {
                        server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }));
            server.awaitTermination();

        } catch (Throwable thrown) {
            logger.log(Level.WARNING, "Server failed", thrown);
        }
    }

    private void processMessageQueue() {
        try {
            while (alive) {
                Thread.sleep(1000);
                NodeMessage message = dequeue();

                if (message == null)
                    continue;

                OnMessageDequeued(message);
            }
        } catch (Throwable thrown) {
            logger.log(Level.WARNING, "processMessageQueue failed", thrown);
        }
    }

    protected abstract void OnMessageDequeued(NodeMessage nodeMessage);

    protected void enqueue(NodeMessage nodeMessage) {
        synchronized (queue) {
            queue.add(nodeMessage);
        }
    }

    private NodeMessage dequeue() {
        synchronized (queue) {
            return queue.poll();
        }
    }

    protected String buildTarget(int port) {
        return "localhost:" + port;
    }
}
