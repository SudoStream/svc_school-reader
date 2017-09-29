package io.sudostream.schoolreader.api.http

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.sudostream.schoolreader.api.kafka.StreamingComponents
import io.sudostream.schoolreader.config.ActorSystemWrapper
import io.sudostream.schoolreader.dao.SchoolReaderDao
import io.sudostream.timetoteach.kafka.serializing.systemwide.model.SchoolsSerializer
import io.sudostream.timetoteach.messages.events.SystemEvent
import io.sudostream.timetoteach.messages.systemwide.model.{School, Schools, SingleSchoolWrapper, SocialNetwork}
import io.sudostream.timetoteach.messages.systemwide.{SystemEventType, TimeToTeachApplication}
import org.apache.avro.io.{DatumWriter, EncoderFactory}
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class HttpRoutes(dao: SchoolReaderDao,
                 actorSystemWrapper: ActorSystemWrapper,
                 streamingComponents: StreamingComponents
                )
  extends Health {
  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log: LoggingAdapter = system.log

  implicit val timeout: Timeout = Timeout(30 seconds)

  private def convertToSocialNetworkName(socialNetworkString: String): SocialNetwork =
    socialNetworkString.toUpperCase match {

      case "FACEBOOK" => SocialNetwork.FACEBOOK
      case "GOOGLE" => SocialNetwork.GOOGLE
      case "TWITTER" => SocialNetwork.TWITTER
      case _ => SocialNetwork.OTHER
    }

  val routes: Route =
    path("api" / "schools") {
      get {
        val initialRequestReceived = Instant.now().toEpochMilli
        log.debug("Called 'api/schools' and now getting All the schools from the DAO")

        val schoolsFuture = dao.extractAllSchools

        Source.fromFuture(schoolsFuture)
          .map {
            elem =>
              log.info(s"Received all ${elem.size} schools from the DAO")

              SystemEvent(
                eventType = SystemEventType.ALL_SCHOOLS_REQUESTED,
                requestFingerprint = UUID.randomUUID().toString,
                requestingSystem = TimeToTeachApplication.HTTP,
                requestingSystemExtraInfo = Option.empty,
                requestingUsername = Option.empty,
                originalUTCTimeOfRequest = initialRequestReceived,
                processedUTCTime = Instant.now().toEpochMilli,
                extraInfo = Option.empty
              )
          }
          .map {
            elem =>
              new ProducerRecord[Array[Byte], SystemEvent](streamingComponents.definedSystemEventsTopic, elem)
          }
          .runWith(Producer.plainSink(streamingComponents.producerSettings))

        onComplete(schoolsFuture) {
          case Success(schools) =>
            log.debug("unwrapping the schools")
            val schoolsWrapper = for {
              school <- schools
              schoolWrapper = SingleSchoolWrapper(school)
            } yield schoolWrapper

            val schoolsAvro = Schools(schools = schoolsWrapper.toList)
            val schoolsSerializer = new SchoolsSerializer
            val schoolsSerialized = schoolsSerializer.serialize("ignore", schoolsAvro )
            complete(HttpEntity(ContentTypes.`application/octet-stream`, schoolsSerialized))
          case Failure(ex) => failWith(ex)
        }


      }

    } ~ health


}
