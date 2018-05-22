/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.scala


import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.io.StdIn

/**
 * Minimum viable sample - single message hello-world actor that just logs greetings
 */
object Sample1 {

  case class Hello(who: String)

  val greetingBehavior: Behavior[Hello] =
    Behaviors.receive { (ctx, hello) =>
      ctx.log.info(s"Hello ${hello.who}!")
      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(greetingBehavior, "my-system")
    val rootActor = system

    rootActor ! Hello("Johan")
    rootActor ! Hello("Devdays Vilnius audience")

    println("Press the any-key to terminate")
    StdIn.readLine()
    system.terminate()
  }
}
