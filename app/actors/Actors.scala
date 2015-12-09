package actors

import javax.inject._

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.AbstractModule
import play.api._
import play.api.libs.concurrent.AkkaGuiceSupport
import java.net.URL

/**
 * Guice module that provides actors.
 *
 * Registered in application.conf.
 */
class Actors extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    // Bind the client connection factory
    bindActorFactory[ClientConnection, ClientConnection.Factory]
  }
}