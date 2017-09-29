package io.sudostream.schoolreader.dao

import org.mongodb.scala.{Document, MongoCollection}

trait MongoDbConnectionWrapper {

  def getSchoolsCollection: MongoCollection[Document]

}
