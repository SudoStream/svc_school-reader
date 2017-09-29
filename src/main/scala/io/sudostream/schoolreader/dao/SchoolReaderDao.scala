package io.sudostream.schoolreader.dao

import io.sudostream.timetoteach.messages.systemwide.model.School

import scala.concurrent.Future

trait SchoolReaderDao {

  def extractAllSchools: Future[Seq[School]]

}
