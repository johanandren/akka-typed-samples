package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import java.io.IOException;

/**
 * Minimum viable sample - single message hello-world actor that just logs greetings
 * Java developers will likely prefer the OO style APIs in sample 2 and forward
 */
public class Sample1 {

  static class Hello {
    public final String who;
    public Hello(String who) {
      this.who = who;
    }
  }

  final static Behavior<Hello> greetingBehavior =
      Behaviors.setup(context ->
          Behaviors.receive(Hello.class)
              .onMessage(Hello.class, message -> {
                context.getLog().info("Hello " + message.who + "!");
                return Behaviors.same();
              }).build()
      );

  public static void main(String[] args) throws IOException {
    ActorSystem<Hello> actorSystem =
        ActorSystem.create(greetingBehavior, "my-system");
    ActorRef<Hello> rootActor = actorSystem;

    rootActor.tell(new Hello("Johan"));
    rootActor.tell(new Hello("Akka talk audience"));
  }
}
