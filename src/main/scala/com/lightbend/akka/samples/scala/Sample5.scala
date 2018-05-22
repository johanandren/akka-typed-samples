/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.scala

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.typed.{Cluster, Join}

import scala.io.StdIn

object Sample5 {

  val serviceKey = ServiceKey[ActivityEvent.type]("alarm")

  sealed trait AlarmMessage
  case class EnableAlarm(pinCode: String) extends AlarmMessage
  case class DisableAlarm(pinCode: String) extends AlarmMessage
  case object ActivityEvent extends AlarmMessage

  def alarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.setup { context =>
      val receptionist = context.system.receptionist
      receptionist ! Receptionist.register(serviceKey, context.self.narrow)
      enabledAlarm(pinCode)
    }

  private def enabledAlarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ActivityEvent =>
          context.log.warning("EOEOEOEOEOE ALARM ALARM!!!")
          Behaviors.same

        case DisableAlarm(`pinCode`) =>
          context.log.info("Correct pin entered, disabling alarm");
          disabledAlarm(pinCode)
        case _ => Behaviors.unhandled
      }

    }

  private def disabledAlarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.receivePartial {
      case (context, EnableAlarm(`pinCode`)) =>
        context.log.info("Correct pin entered, enabling alarm")
        enabledAlarm(pinCode)
    }


  sealed trait TriggerCommand
  object TriggerSensor extends TriggerCommand
  case class AlarmActorChanged(refs: Set[ActorRef[ActivityEvent.type]]) extends TriggerCommand

  def sensorBehavior: Behavior[TriggerCommand] =
    Behaviors.setup { context =>
      val adapter = context.messageAdapter[Receptionist.Listing] {
        case serviceKey.Listing(alarms) =>
          AlarmActorChanged(alarms)
      }
      context.system.receptionist ! Receptionist.Subscribe(serviceKey, adapter)

      var alarms = Set.empty[ActorRef[ActivityEvent.type]]
      Behaviors.receiveMessage {
        case TriggerSensor =>
          if (alarms.isEmpty) context.log.warning("Saw trigger but no alarms known yet")
          else alarms.foreach(_ ! ActivityEvent)
          Behaviors.same
        case AlarmActorChanged(newAlarms) =>
          alarms = newAlarms
          context.log.info("Got alarm actor list update")
          Behaviors.same
      }


    }


  def main(args: Array[String]): Unit = {
    val system1 = ActorSystem.create(alarm("0000"), "my-cluster")
    val system2 = ActorSystem.create(sensorBehavior, "my-cluster")
    val system3 = ActorSystem.create(sensorBehavior, "my-cluster")

    // first join the first node to itself to form a cluster
    val node1 = Cluster(system1)
    node1.manager.tell(Join(node1.selfMember.address))

    // then have node 2 and 3 join that cluster
    val node2 = Cluster(system2)
    node2.manager.tell(Join(node1.selfMember.address))
    val node3 = Cluster(system3)
    node3.manager.tell(Join(node1.selfMember.address))

    while(true) {
      try {
        val chosenNode = StdIn.readLine("Enter 2 or 3 to trigger activity on that node:").toInt
        val system = chosenNode match {
          case 2 => system2
          case 3 => system3
        }
        system ! TriggerSensor
      } catch {
        case ex: Exception =>  // we don't care, loop!
      }
    }
  }

}
