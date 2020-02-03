package com.lightbend.akka.samples.java;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;

import java.util.Collections;
import java.util.Scanner;
import java.util.Set;

/**
 * Distributed burglar alarm sample - a stateful actor that can be in either of two states - enabled or disabled.
 * If there is activity whilst enabled the alarm will sound. Sensors can live on multiple nodes and provide activity
 * events to the alarm.
 */
public class Sample4 {

  // marker interface to use jackson + CBOR for serializing messages sent between nodes
  interface CBORSerializable {}

  static final ServiceKey<Alarm.ActivityEvent> serviceKey =
    ServiceKey.create(Alarm.ActivityEvent.class, "alarm");

  static class Alarm {

    interface Message extends CBORSerializable { }

    static class Enable implements Alarm.Message {
      public final String pinCode;
      public Enable(String pinCode) {
        this.pinCode = pinCode;
      }
    }

    static class Disable implements Alarm.Message {
      public final String pinCode;
      public Disable(String pinCode) {
        this.pinCode = pinCode;
      }
    }

    enum ActivityEvent implements Alarm.Message {
      INSTANCE
    }


    public static Behavior<Alarm.Message> create(String pinCode) {
      return Behaviors.setup(context -> {
        ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
        receptionist.tell(Receptionist.register(serviceKey, context.getSelf().narrow()));
        return new Alarm.EnabledAlarm(context, pinCode);
      });
    }

    static class EnabledAlarm extends AbstractBehavior<Alarm.Message> {

      private final String pinCode;

      public EnabledAlarm(ActorContext<Alarm.Message> context, String pinCode) {
        super(context);
        this.pinCode = pinCode;
      }

      public Receive<Alarm.Message> createReceive() {
        return newReceiveBuilder()
            .onMessage(
                Alarm.Disable.class,
                // predicate
                (disable) -> disable.pinCode.equals(pinCode),
                this::onDisableAlarm)
            .onMessageEquals(Alarm.ActivityEvent.INSTANCE, () -> {
              getContext().getLog().warn("EOEOEOEOEOE ALARM ALARM!!!");
              return this;
            }).build();
      }

      private Behavior<Alarm.Message> onDisableAlarm(Alarm.Disable message) {
        getContext().getLog().info("Correct pin entered, disabling alarm");
        return new Alarm.DisabledAlarm(getContext(), pinCode);
      }

    }

    static class DisabledAlarm extends AbstractBehavior<Alarm.Message> {

      private final String pinCode;

      public DisabledAlarm(ActorContext<Alarm.Message> context, String pinCode) {
        super(context);
        this.pinCode = pinCode;
      }

      @Override
      public Receive<Alarm.Message> createReceive() {
        return newReceiveBuilder()
            .onMessage(
                Alarm.Enable.class,
                // predicate
                (enable) -> enable.pinCode.equals(pinCode),
                this::onEnableAlarm
            ).build();
      }

      private Behavior<Alarm.Message> onEnableAlarm(Alarm.Enable message) {
        getContext().getLog().info("Correct pin entered, enabling alarm");
        return new Alarm.EnabledAlarm(getContext(), pinCode);
      }

    }

  }

  public static class Sensor extends AbstractBehavior<Sensor.Event> {

    public static Behavior<Event> create() {
      return Behaviors.setup(Sensor::new);
    }

    // sensor actor - triggers sends ActivityEvent to all alarms when the sensor is triggered
    interface Event {}
    static class TriggerSensor implements Event {}
    private static class AlarmActorsUpdated implements Event {
      public final Set<ActorRef<Alarm.ActivityEvent>> alarms;
      public AlarmActorsUpdated(Set<ActorRef<Alarm.ActivityEvent>> alarms) {
        this.alarms = alarms;
      }
    }

    private Set<ActorRef<Alarm.ActivityEvent>> alarms = Collections.emptySet();

    public Sensor(ActorContext<Event> context) {
      super(context);

      // we need to transform the messages from the receptionist protocol
      // into our own protocol:
      var adapter = context.messageAdapter(
          Receptionist.Listing.class,
          (listing) -> new AlarmActorsUpdated(listing.getServiceInstances(serviceKey))
      );
      var receptionist = context.getSystem().receptionist();
      receptionist.tell(Receptionist.subscribe(serviceKey, adapter));
    }



    @Override
    public Receive<Event> createReceive() {
      return newReceiveBuilder()
          .onMessage(TriggerSensor.class, this::onTrigger)
          .onMessage(AlarmActorsUpdated.class, this::onAlarmActorUpdate)
          .build();
    }

    private Behavior<Event> onTrigger(TriggerSensor trigger) {
      if (alarms.isEmpty()) getContext().getLog().warn("Saw trigger but no alarms known yet");
      alarms.forEach(alarm ->
          alarm.tell(Alarm.ActivityEvent.INSTANCE)
      );
      return this;
    }

    private Behavior<Event> onAlarmActorUpdate(AlarmActorsUpdated update) {
      getContext().getLog().info("Got alarm actor list update");
      alarms = update.alarms;
      return this;
    }
  }



  // run a three node cluster (normally this wouldn't be in the same main/JVM but on separate ones)
  public static void main(String[] args) {
    var system1 = ActorSystem.create(Alarm.create("0000"), "my-cluster");
    var system2 = ActorSystem.create(Sensor.create(), "my-cluster");
    var system3 = ActorSystem.create(Sensor.create(), "my-cluster");

    // programmatic join, forming is usually done through config or Akka Management
    // first join the first node to itself to form a cluster
    var node1 = Cluster.get(system1);
    node1.manager().tell(Join.create(node1.selfMember().address()));

    // then have node 2 and 3 join that cluster
    var node2 = Cluster.get(system2);
    node2.manager().tell(Join.create(node1.selfMember().address()));
    var node3 = Cluster.get(system3);
    node3.manager().tell(Join.create(node1.selfMember().address()));


    var in = new Scanner(System.in);
    while (true) {
      try {
        System.out.println("Enter 2 or 3 to trigger activity on that node");
        int chosenNode = Integer.parseInt(in.next());
        System.out.println("Triggering sensor on node " + chosenNode);
        if (chosenNode == 2) system2.tell(new Sensor.TriggerSensor());
        else if (chosenNode == 3) system3.tell(new Sensor.TriggerSensor());
      } catch (Exception ex) {
        // we don't care, loop forever!
      }
    }
  }
}
