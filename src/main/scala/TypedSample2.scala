import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object TypedSample2 extends App {

  sealed trait Command
  case class Hello(who: String) extends Command
  case class ChangeGreeting(newGreeting: String) extends Command

  def greetingBehavior(greeting: String): Behavior[Command] =
    Behaviors.receiveMessage {
      case Hello(who) =>
        println(s"$greeting $who!")
        Behaviors.same
      case ChangeGreeting(newGreeting) =>
        greetingBehavior(newGreeting)
    }

  val initialBehavior = greetingBehavior("Hello")
  val system = ActorSystem(initialBehavior, "my-system")

  system ! Hello("Johan")
  system ! ChangeGreeting("Buon Pomeriggio")
  system ! Hello("Scala Italy")

}
