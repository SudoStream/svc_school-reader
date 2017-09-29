package io.sudostream.schoolreader.config

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer


class ActorSystemWrapper(configHelper: ConfigHelper) {
  lazy val system = ActorSystem("school-reader-system", configHelper.config)
  implicit val actorSystem = system
  lazy val materializer = ActorMaterializer()
}
