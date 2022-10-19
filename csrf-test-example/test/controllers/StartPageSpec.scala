package controllers

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Call
import play.api.test.FutureAwaits

class StartPageSpec extends WordSpec with MustMatchers with GuiceOneServerPerSuite {

  override lazy val port = 14681
  override def fakeApplication(): Application = new GuiceApplicationBuilder().build()

  private lazy val serviceUrl = "http://localhost:" + port
  private lazy val wsClient = app.injector.instanceOf[WSClient]

  protected def get(call: Call): WSResponse = {
    await(wsClient.url(serviceUrl + call.url).get())
  }

  protected def post[T](call: Call, payload: String): WSResponse = {
    await(wsClient.url(serviceUrl + call.url).post(payload))
  }

  "GET" should {

    "return 200" in {
      val response: WSResponse = get(controllers.routes.StartController.get())

      response.status mustBe Status.OK
    }
  }

  "POST" should {
    "return 200" in {
      val response: WSResponse = post(controllers.routes.StartController.submit(), "body")

      response.status mustBe Status.OK
    }
  }
}
