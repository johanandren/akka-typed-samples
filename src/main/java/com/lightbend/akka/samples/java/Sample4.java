/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

/**
 * Burglar alarm sample - a stateful actor that can be in either of two states - enabled or disabled.
 * If there is activity whilst enabled the alarm will sound.
 */
public class Sample4 {

  interface AlarmMessage {}

  static class EnableAlarm implements AlarmMessage {
    public final String pinCode;
    public EnableAlarm(String pinCode) {
      this.pinCode = pinCode;
    }
  }

  static class DisableAlarm implements AlarmMessage {
    public final String pinCode;
    public DisableAlarm(String pinCode) {
      this.pinCode = pinCode;
    }
  }

  static class ActivityEvent implements AlarmMessage { }

  public static Behavior<AlarmMessage> enabledAlarm(String pinCode) {
    return Behaviors.receive(AlarmMessage.class)
        .onMessage(
          DisableAlarm.class,
          // predicate
          (disable) -> disable.pinCode.equals(pinCode),
          (context, message) -> {
            context.getLog().info("Correct pin entered, disabling alarm");
            return disabledAlarm(pinCode);
          }
        ).onMessage(ActivityEvent.class, (context, activityEvent) -> {
          context.getLog().warning("EOEOEOEOEOE ALARM ALARM!!!");
          return Behaviors.same();
        }).build();

  }

  public static Behavior<AlarmMessage> disabledAlarm(String pinCode) {
    return Behaviors.receive(AlarmMessage.class)
        .onMessage(EnableAlarm.class,
          // predicate
          (enable) -> enable.pinCode.equals(pinCode),
          (context, message) -> {
            context.getLog().info("Correct pin entered, enabling alarm");
            return enabledAlarm(pinCode);
          }
        ).build();
  }

  public static void main(String[] args) {
    ActorSystem<AlarmMessage> system =
        ActorSystem.create(enabledAlarm("0000"), "my-system");
    system.tell(new ActivityEvent());
    system.tell(new DisableAlarm("1234"));
    system.tell(new ActivityEvent());
    system.tell(new DisableAlarm("0000"));
    system.tell(new ActivityEvent());
    system.tell(new EnableAlarm("0000"));
    system.tell(new ActivityEvent());
  }
}
