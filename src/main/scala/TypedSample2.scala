import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object TypedSample2 extends App {

  sealed trait Command
  case class Hello(who: String) extends Command
  case class ChangeGreeting(newGreeting: String)
    extends Command

  def greeter(greeting: String): Behavior[Command] =
    Behaviors.receiveMessage {
      case Hello(who) =>
        println(s"$greeting $who!")
        Behaviors.same
      case ChangeGreeting(newGreeting) =>
        greeter(newGreeting)
    }

  val system = ActorSystem(
    greeter("Hello"),
    "my-system")

  system ! Hello("World")
  system ! ChangeGreeting("Buon Pomeriggio")
  system ! Hello("Scala Italy")

}
