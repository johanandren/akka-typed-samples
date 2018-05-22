/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import java.io.IOException;

/**
 * Minimum viable sample - single message hello-world actor that just logs greetings
 */
public class Sample1 {

  static class Hello {
    public final String who;
    public Hello(String who) {
      this.who = who;
    }
  }

  final static Behavior<Hello> greetingBehavior =
    Behaviors.receive(Hello.class)
      .onMessage(Hello.class, (context, message) -> {
        context.getLog().info("Hello " + message.who + "!");
        return Behavior.same();
      }).build();

  public static void main(String[] args) throws IOException {
    ActorSystem<Hello> actorSystem =
        ActorSystem.create(greetingBehavior, "my-system");
    ActorRef<Hello> rootActor = actorSystem;
    rootActor.tell(new Hello("Johan"));
    rootActor.tell(new Hello("Devdays Vilnius audience"));

    System.out.println("Press that any-key to terminate");
    System.in.read();
    actorSystem.terminate();
  }
}
