package io.sudostream.schoolreader.dao

import org.mongodb.scala.{Document, FindObservable, MongoCollection}

import scala.concurrent.Future

class MongoFindQueriesImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoFindQueriesProxy {

  val schoolsCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getSchoolsCollection

  override def findAllSchools: Future[Seq[Document]] = {
    val schoolsMongoDocuments: FindObservable[Document] = schoolsCollection.find(Document())
    schoolsMongoDocuments.toFuture()
  }

}
