package io.sudostream.schoolreader.api.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import io.sudostream.timetoteach.kafka.serializing.SystemEventSerializer
import io.sudostream.schoolreader.config.{ActorSystemWrapper, ConfigHelper}
import org.apache.kafka.common.serialization.ByteArraySerializer

import scala.concurrent.ExecutionContextExecutor

class StreamingComponents(configHelper: ConfigHelper, actorSystemWrapper: ActorSystemWrapper) {
  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log = system.log

  lazy val kafkaProducerBootServers = configHelper.config.getString("akka.kafka.producer.bootstrapservers")

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new SystemEventSerializer)
    .withBootstrapServers(kafkaProducerBootServers)

  def definedSystemEventsTopic: String = {
    val sink_topic = configHelper.config.getString("school-reader.system_events_topic")
    log.info(s"Sink topic is '$sink_topic'")
    sink_topic
  }

}
