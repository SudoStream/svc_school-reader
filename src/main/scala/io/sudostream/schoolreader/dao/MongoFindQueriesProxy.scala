package io.sudostream.schoolreader.dao

import org.mongodb.scala.Document

import scala.concurrent.Future

trait MongoFindQueriesProxy {

  def findAllSchools: Future[Seq[Document]]

}
