package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * Burglar alarm sample - a stateful actor that can be in either of two states - enabled or disabled.
 * If there is activity whilst enabled the alarm will sound.
 */
public class Sample3 {

  static class Alarm {

    interface Message { }

    static class Enable implements Message {
      public final String pinCode;
      public Enable(String pinCode) {
        this.pinCode = pinCode;
      }
    }

    static class Disable implements Message {
      public final String pinCode;
      public Disable(String pinCode) {
        this.pinCode = pinCode;
      }
    }

    enum ActivityEvent implements Message {
      INSTANCE
    }


    public static Behavior<Message> create(String pinCode) {
      return Behaviors.setup(context -> new EnabledAlarm(context, pinCode));
    }

    static class EnabledAlarm extends AbstractBehavior<Message> {

      private final String pinCode;

      public EnabledAlarm(ActorContext<Message> context, String pinCode) {
        super(context);
        this.pinCode = pinCode;
      }

      public Receive<Message> createReceive() {
        return newReceiveBuilder()
            .onMessage(
                Disable.class,
                // predicate
                (disable) -> disable.pinCode.equals(pinCode),
                this::onDisableAlarm)
            .onMessageEquals(ActivityEvent.INSTANCE, () -> {
              getContext().getLog().warn("EOEOEOEOEOE ALARM ALARM!!!");
              return this;
            }).build();
      }

      private Behavior<Message> onDisableAlarm(Disable message) {
        getContext().getLog().info("Correct pin entered, disabling alarm");
        return new DisabledAlarm(getContext(), pinCode);
      }

    }

    static class DisabledAlarm extends AbstractBehavior<Message> {

      private final String pinCode;

      public DisabledAlarm(ActorContext<Message> context, String pinCode) {
        super(context);
        this.pinCode = pinCode;
      }

      @Override
      public Receive<Message> createReceive() {
        return newReceiveBuilder()
            .onMessage(
                Enable.class,
                // predicate
                (enable) -> enable.pinCode.equals(pinCode),
                this::onEnableAlarm
            ).build();
      }

      private Behavior<Message> onEnableAlarm(Enable message) {
        getContext().getLog().info("Correct pin entered, enabling alarm");
        return new EnabledAlarm(getContext(), pinCode);
      }

    }

  }

  public static void main(String[] args) {
    var system = ActorSystem.create(Alarm.create("0000"), "my-system");
    system.tell(Alarm.ActivityEvent.INSTANCE);
    system.tell(new Alarm.Disable("1234"));
    system.tell(Alarm.ActivityEvent.INSTANCE);
    system.tell(new Alarm.Disable("0000"));
    system.tell(Alarm.ActivityEvent.INSTANCE);
    system.tell(new Alarm.Enable("0000"));
    system.tell(Alarm.ActivityEvent.INSTANCE);
  }
}
