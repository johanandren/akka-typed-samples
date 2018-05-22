/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.MutableBehavior;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;

import java.io.Serializable;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;

/**
 * Distributed burglar alarm sample - a stateful actor that can be in either of two states - enabled or disabled.
 * If there is activity whilst enabled the alarm will sound. Sensors can live on multiple nodes and provide activity
 * events to the alarm.
 */
public class Sample5 {

  static final ServiceKey<ActivityEvent> serviceKey =
    ServiceKey.create(ActivityEvent.class, "alarm");

  // alarm actor

  // note that Java serialization should never be used for anything but demos!
  interface AlarmMessage extends Serializable {}

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

  public static Behavior<AlarmMessage> alarm(String pinCode) {
    return Behaviors.setup((context) -> {
      ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
      receptionist.tell(Receptionist.register(serviceKey, context.getSelf().narrow()));
      return enabledAlarm(pinCode);
    });
  }

  private static Behavior<AlarmMessage> enabledAlarm(String pinCode) {
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

  private static Behavior<AlarmMessage> disabledAlarm(String pinCode) {
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



  // sensor actor - triggers sends ActivityEvent to all alarms when the sensor is triggered
  interface TriggerCommand {}
  static class TriggerSensor implements TriggerCommand {}
  private static class AlarmActorUpdate implements TriggerCommand {
    public final Set<ActorRef<ActivityEvent>> alarms;
    public AlarmActorUpdate(Set<ActorRef<ActivityEvent>> alarms) {
      this.alarms = alarms;
    }
  }


  public static class SensorBehavior extends MutableBehavior<TriggerCommand> {

    private final ActorContext<TriggerCommand> context;
    private Set<ActorRef<ActivityEvent>> alarms = Collections.EMPTY_SET;

    public SensorBehavior(ActorContext<TriggerCommand> context) {
      this.context = context;

      // we need to transform the messages from the receptionist protocol
      // into our own protocol:
      ActorRef<Receptionist.Listing> adapter = context.messageAdapter(
          Receptionist.Listing.class,
          (listing) -> new AlarmActorUpdate(listing.getServiceInstances(serviceKey))
      );
      ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
      receptionist.tell(Receptionist.subscribe(serviceKey, adapter));
    }

    @Override
    public Behaviors.Receive<TriggerCommand> createReceive() {
      return receiveBuilder()
          .onMessage(TriggerSensor.class, this::onTrigger)
          .onMessage(AlarmActorUpdate.class, this::onAlarmActorUpdate)
          .build();
    }

    private Behavior<TriggerCommand> onTrigger(TriggerSensor trigger) {
      final ActivityEvent activityEvent = new ActivityEvent();
      if (alarms.isEmpty()) context.getLog().warning("Saw trigger but no alarms known yet");
      alarms.forEach((alarm) ->
        alarm.tell(activityEvent)
      );
      return Behaviors.same();
    }

    private Behavior<TriggerCommand> onAlarmActorUpdate(AlarmActorUpdate update) {
      context.getLog().info("Got alarm actor list update");
      alarms = update.alarms;
      return Behaviors.same();
    }
  }

  public static Behavior<TriggerCommand> sensorBehavior() {
    return Behaviors.setup(SensorBehavior::new);
  }


  // run it all (normally this wouldn't be in the same main/JVM but on separate ones
  public static void main(String[] args) throws Exception {

    ActorSystem<AlarmMessage> system1 =
      ActorSystem.create(alarm("0000"), "my-cluster");
    ActorSystem<TriggerCommand> system2 =
      ActorSystem.create(sensorBehavior(), "my-cluster");
    ActorSystem<TriggerCommand> system3 =
        ActorSystem.create(sensorBehavior(), "my-cluster");

    // first join the first node to itself to form a cluster
    Cluster node1 = Cluster.get(system1);
    node1.manager().tell(Join.create(node1.selfMember().address()));

    // then have node 2 and 3 join that cluster
    Cluster node2 = Cluster.get(system2);
    node2.manager().tell(Join.create(node1.selfMember().address()));
    Cluster node3 = Cluster.get(system3);
    node3.manager().tell(Join.create(node1.selfMember().address()));


    Scanner in = new Scanner(System.in);
    while (true) {
      try {
        System.out.println("Enter 2 or 3 to trigger activity on that node");
        int chosenNode = Integer.parseInt(in.next());
        System.out.println("Triggering sensor on node " + chosenNode);
        if (chosenNode == 2) system2.tell(new TriggerSensor());
        else if (chosenNode == 3) system3.tell(new TriggerSensor());
      } catch (Exception ex) {
        // we don't care, loop!
      }
    }
  }
}
