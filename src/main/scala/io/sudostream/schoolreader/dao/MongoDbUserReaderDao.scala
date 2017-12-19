package io.sudostream.schoolreader.dao

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.schoolreader.config.ActorSystemWrapper
import io.sudostream.timetoteach.messages.systemwide.model._
import org.mongodb.scala.Document

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

sealed class MongoDbSchoolReaderDao(mongoFindQueriesProxy: MongoFindQueriesProxy,
                                    actorSystemWrapper: ActorSystemWrapper) extends SchoolReaderDao {


  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log: LoggingAdapter = system.log

  override def extractAllSchools : Future[Seq[School]] = {
    val allSchoolsFutureDocuments: Future[Seq[Document]] = mongoFindQueriesProxy.findAllSchools
    createSchoolsFromDocuments(allSchoolsFutureDocuments)
  }

  private def createSchoolsFromDocuments( schoolDocuments: Future[Seq[Document]]) : Future[Seq[School]] = {
    schoolDocuments map {
      allSchoolsAsMongoDocuments =>
        val seqOfSchools: Seq[Try[School]] =
          for {
            singleSchoolDoc <- allSchoolsAsMongoDocuments
            singleSchool: Try[School] = createSchoolFromSingleMongoDocument(singleSchoolDoc)
          } yield singleSchool

        val failures = seqOfSchools.filter(singleTry => singleTry.isFailure)

        if (failures.nonEmpty) {
          failures foreach (fail => log.error(s"problem with $fail"))
          val errorMsg = "Failed to correctly parse schools from database"
          log.error(errorMsg)
          return Future.failed(new RuntimeException(errorMsg))
        } else {
          seqOfSchools map { schoolTry => schoolTry.get }
        }
    }
  }


  def extractLocalAuthority(localAuthorityString: String): LocalAuthority = {
    localAuthorityString.toUpperCase match {
      case "ABERDEEN_CITY" => LocalAuthority.SCOTLAND__ABERDEEN_CITY
      case "ABERDEENSHIRE" => LocalAuthority.SCOTLAND__ABERDEENSHIRE
      case "ANGUS" => LocalAuthority.SCOTLAND__ANGUS
      case "ARGYLL_AND_BUTE" => LocalAuthority.SCOTLAND__ARGYLL_AND_BUTE
      case "COMHAIRLE_NAN_EILEAN_SIAR" => LocalAuthority.SCOTLAND__COMHAIRLE_NAN_EILEAN_SIAR
      case "EILEAN_SIAR" => LocalAuthority.SCOTLAND__COMHAIRLE_NAN_EILEAN_SIAR
      case "CLACKMANNANSHIRE" => LocalAuthority.SCOTLAND__CLACKMANNANSHIRE
      case "DUMFRIES_AND_GALLOWAY" => LocalAuthority.SCOTLAND__DUMFRIES_AND_GALLOWAY
      case "DUNDEE_CITY" => LocalAuthority.SCOTLAND__DUNDEE_CITY
      case "EAST_AYRSHIRE" => LocalAuthority.SCOTLAND__EAST_AYRSHIRE
      case "EAST_DUMBARTONSHIRE" => LocalAuthority.SCOTLAND__EAST_DUMBARTONSHIRE
      case "EAST_DUNBARTONSHIRE" => LocalAuthority.SCOTLAND__EAST_DUMBARTONSHIRE
      case "EDINBURGH_CITY" => LocalAuthority.SCOTLAND__EDINBURGH_CITY
      case "EAST_LOTHIAN" => LocalAuthority.SCOTLAND__EAST_LOTHIAN
      case "EAST_RENFREWSHIRE" => LocalAuthority.SCOTLAND__EAST_RENFREWSHIRE
      case "FALKIRK" => LocalAuthority.SCOTLAND__FALKIRK
      case "FIFE" => LocalAuthority.SCOTLAND__FIFE
      case "GLASGOW" => LocalAuthority.SCOTLAND__GLASGOW
      case "GLASGOW_CITY" => LocalAuthority.SCOTLAND__GLASGOW
      case "HIGHLAND" => LocalAuthority.SCOTLAND__HIGHLAND
      case "INVERCLYDE" => LocalAuthority.SCOTLAND__INVERCLYDE
      case "MIDLOTHIAN" => LocalAuthority.SCOTLAND__MIDLOTHIAN
      case "MORAY" => LocalAuthority.SCOTLAND__MORAY
      case "NORTH_AYRSHIRE" => LocalAuthority.SCOTLAND__NORTH_AYRSHIRE
      case "NORTH_LANARKSHIRE" => LocalAuthority.SCOTLAND__NORTH_LANARKSHIRE
      case "ORKNEY" => LocalAuthority.SCOTLAND__ORKNEY
      case "PERTH_AND_KINROSS" => LocalAuthority.SCOTLAND__PERTH_AND_KINROSS
      case "RENFREWSHIRE" => LocalAuthority.SCOTLAND__RENFREWSHIRE
      case "SCOTTISH_BORDERS" => LocalAuthority.SCOTLAND__SCOTTISH_BORDERS
      case "SHETLAND_ISLANDS" => LocalAuthority.SCOTLAND__SHETLAND_ISLANDS
      case "SOUTH_AYRSHIRE" => LocalAuthority.SCOTLAND__SOUTH_AYRSHIRE
      case "SOUTH_LANARKSHIRE" => LocalAuthority.SCOTLAND__SOUTH_LANARKSHIRE
      case "STIRLING" => LocalAuthority.SCOTLAND__STIRLING
      case "WEST_DUMBARTONSHIRE" => LocalAuthority.SCOTLAND__WEST_DUMBARTONSHIRE
      case "WEST_LOTHIAN" => LocalAuthority.SCOTLAND__WEST_LOTHIAN
      case "GRANT-MAINTAINED" => LocalAuthority.SCOTLAND__GRANT_MAINTAINED

      case "OTHER" => LocalAuthority.OTHER
      case _ => LocalAuthority.UNKNOWN

    }
  }

  def extractCountry(countryStr: String): Country = {
    countryStr.toUpperCase match {
      case "EIRE" => Country.EIRE
      case "ENGLAND" => Country.ENGLAND
      case "NORTHERN_IRELAND" => Country.NORTHERN_IRELAND
      case "SCOTLAND" => Country.SCOTLAND
      case "WALES" => Country.WALES
      case "OTHER" => Country.OTHER
      case _ => Country.UNKNOWN
    }
  }

  def createSchoolFromSingleMongoDocument(singleSchoolDoc: Document): Try[School] = {
    Try {
      val schoolId: String = singleSchoolDoc.getString("_id")
      val schoolName: String = singleSchoolDoc.getString("name")
      val schoolAddress: String = singleSchoolDoc.getString("address")
      val schoolPostCode: String = singleSchoolDoc.getString("postCode")
      val schoolTelephone: String = singleSchoolDoc.getString("telephone")
      val schoolLocalAuthority: LocalAuthority = extractLocalAuthority(singleSchoolDoc.getString("localAuthority"))
      val schoolCountry: Country = extractCountry(singleSchoolDoc.getString("country"))

      School(
        id = schoolId,
        name = schoolName,
        address = schoolAddress,
        postCode = schoolPostCode,
        telephone = schoolTelephone,
        localAuthority = schoolLocalAuthority,
        country = schoolCountry
      )
    }
  }

}
