/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.MutableBehavior;

/**
 * Same as Sample2 but using a more oo-like API
 */
public class Sample3 {

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

  static class MutableGreetingBehavior extends MutableBehavior<Command> {

    private final ActorContext<Command> context;
    private String greeting;

    public MutableGreetingBehavior(String initialGreeting, ActorContext<Command> context) {
      this.context = context;
      greeting = initialGreeting;
    }

    @Override
    public Behaviors.Receive<Command> createReceive() {
      return receiveBuilder()
          .onMessage(Hello.class, this::onHello)
          .onMessage(ChangeGreeting.class, this::onChangeGreeting)
          .build();
    }

    private Behavior<Command> onHello(Hello hello) {
      context.getLog().info(greeting + " " + hello.who + "!");
      return Behaviors.same();
    }

    private Behavior<Command> onChangeGreeting(ChangeGreeting changeGreeting) {
      greeting = changeGreeting.newGreeting;
      return Behaviors.same();
    }
  }

  public static void main(String[] args) {
    var system = ActorSystem.<Command>create(
        Behaviors.setup((context) -> new MutableGreetingBehavior("Hello", context)),
        "my-system"
    );
    system.tell(new Hello("Johan"));
    system.tell(new ChangeGreeting("Sveiki"));
    system.tell(new Hello("Devdays Vilnius audience"));
  }
}
