/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.scala

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

/**
 * Shows multi-message protocol, and keepin a constant state, but changing it by changing behavior
 */
object Sample2 {

  sealed trait Command
  case class Hello(who: String) extends Command
  case class ChangeGreeting(newGreeting: String) extends Command

  def dynamicGreetingBehavior(greeting: String): Behavior[Command] =
    Behaviors.receive { (ctx, message) =>
      message match {
        case Hello(who) =>
          ctx.log.info(s"$greeting ${who}!")
          Behaviors.same
        case ChangeGreeting(newGreeting) =>
          dynamicGreetingBehavior(newGreeting)
      }
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(dynamicGreetingBehavior("Hello"), "my-system")

    system ! Hello("Johan")
    system ! ChangeGreeting("Hej")
    system ! Hello("Øredec audience")
  }
}
