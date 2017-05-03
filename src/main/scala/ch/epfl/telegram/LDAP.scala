package ch.epfl.telegram

import ch.epfl.telegram.models.EPFLUser
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.sdk._
import com.unboundid.ldap.sdk.SearchScope

import scala.concurrent.Await
import scala.util.Try
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Populates the 'user / epfl' index mirroring the whole EPFL directory.
  * Login information (linked accounts) will be lost.
  */
object LDAP extends App {

  val logger = Logger(getClass)

  def scrap(filter: String): Array[SearchResultEntry] = {
    val ldap = new LDAPConnection()
    try
      ldap.connect("ldap.epfl.ch", 389)
    catch {
      case NonFatal(e)  =>
        logger.error("LDAP connection failed")
        throw e
    }
    require (ldap.isConnected)
    val t = ldap.search("o=epfl,c=ch", SearchScope.SUB, filter)
    t.getSearchEntries.toArray(Array[SearchResultEntry]())
  }

  def toEPFLUser(sre: SearchResultEntry): Option[EPFLUser] = {

    // LDAP specific attributes.
    val wantedAtts = Set(
      "uniqueIdentifier",
      "givenName",
      "sn",
      "displayName",
      "ou",
      "employeeType",
      "mail",
      "uid")

    val atts = for {
      a <- sre.getAttributes.toArray(Array[Attribute]())
      // Ignored binary attributes (e.g. certificates)
      if !a.getName().contains("binary") && wantedAtts.contains(a.getName())
    } yield {
      val name = a.getName()
      val value = if (name != "uid") a.getValues().last else a.getValues().head
      (name -> value)
    }

    val data = atts.toMap

    Try (
      EPFLUser(
        id = data("uniqueIdentifier").toInt,
        telegramInfo = None,
        firstName =  data("givenName"),
        name = data("sn"),
        displayName = data("displayName"),
        employeeType = Some(data("employeeType")),
        email = data("mail"),
        gaspar = data("uid").takeWhile(x => x != '@'),
        where = data("ou")
      )
    ).toOption
  }

  val entries = scrap("(employeeType=*)")
  val users = entries.flatMap(x => toEPFLUser(x))
  logger.debug("Inserting " + users.size + " EPFL users...")
  Await.result(EPFLUser.putUserSeq(users), 1.minute)
}
