package pong;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import static akka.pattern.Patterns.ask;
import org.junit.Test;
import scala.concurrent.Future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static scala.compat.java8.FutureConverters.*;

public class PongActorTest {
    ActorSystem actorSystem = ActorSystem.create();
    ActorRef actorRef = this.actorSystem.actorOf(Props.create(PongActor.class));

    @Test
    public void shouldReplyToPingWithPong() throws Exception {

        // short way
        final CompletableFuture<String> jFuture = askPong("Ping");
        assert(jFuture.get(1000, TimeUnit.MILLISECONDS)).equals("Pong");
    }

    @Test(expected = ExecutionException.class)
    public void shouldReplyToUnknownMessageWithFailure() throws Exception {

        // long way
        Future sFuture = ask(actorRef, "unknown", 1000);
        final CompletionStage<String> cs = toJava(sFuture);
        final CompletableFuture<String> jFuture = (CompletableFuture<String>) cs;
        jFuture.get(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void printToConsole() throws Exception {
        this.askPong("Ping").
                thenAccept(x -> System.out.println("replied with: " + x));

        System.out.println("Sleeping...");
        Thread.sleep(100);                      // do some "work", don't wait for a response

        // thenAccept will happen after the sleep
    }

    @Test
    public void composeAsync() throws Exception {
        // async call followed by another async call
        CompletableFuture<String> cs = this.askPong("Ping").
                thenCompose(x -> this.askPong("Ping"));

        cs.thenAccept(x -> System.out.println("replied with2: " + x));

        Thread.sleep(100);
    }

    @Test
    public void handleError() throws Exception {
        this.askPong("cause error").
                handle((x, t) -> {
                    if (t != null) {
                        System.out.println("Error: " + t);
                        return null;
                    }

                    return null;
                });

        Thread.sleep(100);
    }

    @Test
    public void handleSuccess() throws Exception {
        CompletableFuture<String> cs = this.askPong("Ping").
                handle((x, t) -> {
                    if (t != null) {
                        System.out.println("Error: " + t);
                        return null;
                    }

                    return x;
                });

        cs.thenAccept(x -> System.out.println("Success: " + x));

        Thread.sleep(100);
    }

    @Test
    public void recoverError() throws Exception {
        CompletableFuture<String> cs = this.askPong("cause error").
                exceptionally(t -> {
                   return "default";
                });

        cs.thenAccept(x -> System.out.println("Recovery: " + x));

        Thread.sleep(100);
    }

    // TODO Recovering from failure asynchronously

    public CompletableFuture<String> askPong(String message) {
        Future sFuture = ask(this.actorRef, message, 1000);
        return (CompletableFuture<String>) toJava(sFuture);
    }
}
