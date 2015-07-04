package com.example

import java.util.UUID

import argonaut.Argonaut._
import argonaut.CursorHistory
import com.typesafe.config.{ConfigFactory, Config}
import unfiltered.filter.request.ContextPath
import unfiltered.request._
import unfiltered.response._

import unfiltered.directives._, Directives._

import scala.concurrent.Await
import scalaz.{Validation, \/}

case class Token(accessToken: String,
                 tokenType: String,
                 expiresIn: Int)
object Token {
  implicit val codec = casecodec3(Token.apply, Token.unapply)("access_token", "token_type", "expires_in")
}

case class Me(id: String,
              firstName: String,
              gender: String,
              lastName: String,
              link: String,
              locale: String,
              name: String,
              timezone: Int,
              updatedTime: String,
              verified: Boolean)
object Me {
  implicit val codec = casecodec10(Me.apply, Me.unapply)(
    "id", "first_name", "gender", "last_name", "link", "locale", "name", "timezone", "updated_time", "verified"
  )
}

//** unfiltered plan */
class App extends unfiltered.filter.Plan {
  val conf: Config = ConfigFactory.load("facebook-credentials.conf")

  val appId = conf.getString("app-id")
  val clientSecret = conf.getString("client-secret")

  val redirectUrl = "http://localhost:7070/oauth_callback"


  def intent = Directive.Intent {
    case ContextPath(_, Seg("oauth_callback" :: Nil)) ⇒
      for {
        _ ← GET
        code ← data.as.String named "code"
        state ← data.as.String named "state"
      } yield {
        println(s"code: $code")
        println(s"state: $state")

        // get access-token
        import dispatch._, Defaults._

        val req = url("https://graph.facebook.com/v2.3/oauth/access_token") <<?
          Map(
            "client_id" → appId,
            "redirect_uri" → redirectUrl,
            "client_secret" → clientSecret,
            "code" → code.get
          )

        println(s"req: ${req.toRequest.getRawUrl}")

        import scala.concurrent.duration._
        val response: String = Await.result(Http(req OK as.String), 5.seconds)

        val token: Token = response.decodeOption[Token].get

        println("token: " + token)

        val meReq = url("https://graph.facebook.com/v2.3/me")
          .addQueryParameter("access_token", token.accessToken)

        println(meReq.toRequest.getRawUrl)

        val meResp = Await.result(Http(meReq OK as.String), 5.seconds)

        val me = meResp.decodeOption[Me].get
        println(me)

        Ok ~> ResponseString(me.asJson.spaces2)
      }

    case ContextPath(_, Seg("facebook_login" :: Nil)) ⇒
      for {
        _ ← GET
      } yield {
        val state = UUID.randomUUID()
        println(s"state: $state")
        val url = s"https://www.facebook.com/dialog/oauth?" +
          Seq(
            s"client_id=$appId",
            s"redirect_uri=$redirectUrl",
            s"state=${state.toString}",
            s"response_type=code"
          ).mkString("&")
        Html5(
          <html>
            <head>
              <title>Unfiltered Facebook Auth</title>
              <link rel="stylesheet" type="text/css" href="/assets/css/app.css"/>
            </head>
            <body>
              <a href={url}>logg inn med facebook</a>
            </body>
          </html>
        )
      }
  }
}

/** embedded server */
object Server {
  def main(args: Array[String]) {
    val port: Int = 7070

    unfiltered.jetty.Server.http(port).context("/assets") {
      _.resources(new java.net.URL(getClass.getResource("/www/css"), "."))
    }.plan(new App).run()
  }
}
