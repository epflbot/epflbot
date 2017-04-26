package ch.epfl.telegram

import ch.epfl.telegram.models.EPFLUser
import com.unboundid.ldap.sdk._
import com.unboundid.ldap.sdk.SearchScope

import scala.util.Try
import scala.util.control.NonFatal

import scala.concurrent.ExecutionContext.Implicits.global

object LDAP extends App {

  def scrap(filter: String) = {
    val ldap = new LDAPConnection()
    try
      ldap.connect("ldap.epfl.ch", 389)
    catch {
      case NonFatal(e)  =>
        System.err.println("Ldap exception")
    }
    require (ldap.isConnected)
    val t = ldap.search("o=epfl,c=ch", SearchScope.SUB, filter)
    t.getSearchEntries.toArray(Array[SearchResultEntry]())
  }

  def toEPFLUser(sre: SearchResultEntry): Option[EPFLUser] = {
      val wantedAtts = Set("uniqueIdentifier", "givenName","sn", "displayName", "ou", "employeeType", "mail", "uid")
    val atts = for {
      a <- sre.getAttributes.toArray(Array[Attribute]())
      if !a.getName().contains("binary") && wantedAtts.contains(a.getName())
    }
      yield {
        val name = a.getName()
        val value = if(name != "uid") a.getValues().last else a.getValues().head
        (name -> value)
      }
    val data = atts.toMap

    Try (
      EPFLUser(
        id = None,
        sciper = data("uniqueIdentifier").toInt,
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
  EPFLUser.putUserBySciper(users)
}
