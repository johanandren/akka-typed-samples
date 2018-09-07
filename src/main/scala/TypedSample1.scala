import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object TypedSample1 extends App {

  case class Hello(who: String)

  val greetingBehavior: Behavior[Hello] = Behaviors.receiveMessage {
    case Hello(who) =>
      println(s"Hello $who!")
      Behaviors.same
  }

  val system = ActorSystem(greetingBehavior, "my-system")

  system ! Hello("Johan")
  system ! Hello("Scala Italy")


  // (messages delivered async, main completes, system keeps running)
}


// with explicit types
object TypedSample1B extends App {

  case class Hello(who: String)

  val greetingBehavior: Behavior[Hello] = Behaviors.receiveMessage[Hello] {
    case Hello(who) =>
      println(s"Hello $who!")
      Behaviors.same
  }

  val system: ActorSystem[Hello] = ActorSystem[Hello](greetingBehavior, "my-system")
  val ref: ActorRef[Hello] = system
  system ! Hello("Johan")
  system ! Hello("Scala Italy")


  // (messages delivered async, main completes, system keeps running)
}
