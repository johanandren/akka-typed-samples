package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.IOException;

/**
 * Shows multi-message protocol, and the OO style API
 */
public class Sample2 {

  static final class DynamicGreeter extends AbstractBehavior<DynamicGreeter.Command> {

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

    static Behavior<Command> create(String initialGreeting) {
      return Behaviors.setup(context -> new DynamicGreeter(context, initialGreeting));
    }

    private String greeting;

    private DynamicGreeter(ActorContext<Command> context, String initialGreeting) {
      super(context);
      greeting = initialGreeting;
    }

    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(Hello.class, this::onHello)
          .onMessage(ChangeGreeting.class, this::onChangeGreeting)
          .build();
    }


    private Behavior<Command> onHello(Hello command) {
      getContext().getLog().info(greeting + " " + command.who + "!");
      return this;
    }

    private Behavior<Command> onChangeGreeting(ChangeGreeting command) {
      greeting = command.newGreeting;
      return this;
    }

  }

  public static void main(String[] args) throws IOException {
    var system = ActorSystem.create(DynamicGreeter.create("Hello"), "my-system");
    system.tell(new DynamicGreeter.Hello("Johan"));
    system.tell(new DynamicGreeter.ChangeGreeting("Hej"));
    system.tell(new DynamicGreeter.Hello("Akka talk audience"));
  }

}
