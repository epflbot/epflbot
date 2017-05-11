package ch.epfl.telegram.commands

import ch.epfl.telegram.models.EPFLUser
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.TelegramBot
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, ParseMode}
import info.mukel.telegrambot4s.models._

/**
  * Enables searching the EPFL directory within the bot. using inine queries
  * e.g. @EpflBot query
  *
  * Users linked to @EPFLBot will have a 🤖🤖🤖🤖robot face prepended.
  * Clicking the photo opens the EPFL profile page.
  * Clicking the text leaves a user-badge with contact info; including a link
  * to open a Telegram chat if the user has authenticated his EPFL account.
  */
trait InlineEpflDirectory extends TelegramBot {

  def photoUrl(sciper: Int): String =
    s"https://people.epfl.ch/cgi-bin/people/getPhoto?id=$sciper"

  def epflProfileUrl(sciper: Int): String =
    s"https://people.epfl.ch/$sciper"

  def telegramChatWith(username: String): String =
    s"https://t.me/$username"

  override def onInlineQuery(inlineQuery: InlineQuery): Unit = {
    super.onInlineQuery(inlineQuery)

    for (searchResults <- EPFLUser.searchDirectory(inlineQuery.query)) {

      val results: Seq[InlineQueryResult] =
        for ((epflUser, index) <- searchResults.zipWithIndex)
          yield {
            val profileUrl = epflProfileUrl(epflUser.sciper)

            val userBadge =
              "Profile: " + epflUser.displayName.altWithUrl(profileUrl) + "\n" +
                "Email: " + epflUser.email + {

                val telegramLabel = for {
                  ti               <- epflUser.telegramInfo
                  telegramUsername <- ti.username
                } yield
                  "\n\n" + s"Open chat with ${epflUser.firstName}"
                    .altWithUrl(telegramChatWith(telegramUsername))

                telegramLabel.getOrElse("")
              }

            val robotFace = "\uD83E\uDD16"

            // Prepend a robot emoji if the user has linked an EPFL account.
            val fullTitle = (if (epflUser.isAvailableOnTelegram) robotFace + " " else "") +
              epflUser.displayName

            InlineQueryResultArticle(
              index.toString,
              fullTitle,
              InputTextMessageContent(
                userBadge,
                parseMode = ParseMode.Markdown,
                // Page preview just shows your (most likely fake) profile picture.
                disableWebPagePreview = true
              ),
              thumbUrl = photoUrl(epflUser.sciper),
              url = profileUrl,
              description = epflUser.where
            )
          }

      // TODO: Increase cacheTime in production.
      request(AnswerInlineQuery(inlineQuery.id, results, cacheTime = 10))
    }
  }
}
