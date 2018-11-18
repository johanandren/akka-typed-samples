/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.java;

import akka.actor.Actor;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import java.io.IOException;

/**
 * Shows multi-message protocol, and keepin a constant state, but changing it by changing behavior
 */
public class Sample2 {

  interface Command {}

  static class ChangeGreeting implements Command {
    public final String newGreeting;
    public ChangeGreeting(String newGreeting) {
      this.newGreeting = newGreeting;
    }
  }

  static class Hello implements Command {
    public final String who;
    public Hello(String who) {
      this.who = who;
    }
  }

  public static Behavior<Command> dynamicGreetingBehavior(String greeting) {
    return Behaviors.receive(Command.class)
        .onMessage(Hello.class, (context, message) -> {
          context.getLog().info(greeting + " " + message.who + "!");
          return Behavior.same();
        }).onMessage(ChangeGreeting.class, (context, changeGreeting) ->
          dynamicGreetingBehavior(changeGreeting.newGreeting)
        ).build();
  }

  public static void main(String[] args) throws IOException {
    ActorSystem<Command> system =
        ActorSystem.create(dynamicGreetingBehavior("Hello"), "my-system");
    system.tell(new Hello("Johan"));
    system.tell(new ChangeGreeting("Hej"));
    system.tell(new Hello("Øredev audience"));
  }

}
