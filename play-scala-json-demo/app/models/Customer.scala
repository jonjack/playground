package models

import play.api.libs.json._

case class Customer (
  val id: String,
  val title: String,
  val firstName: String,
  val surname: String,
  val dateOfBirth: String,
  val primaryTelephoneNumber: String,
  val primaryTelephoneType: String
)
{
  implicit val customerWrites = Json.writes[Customer]
  val asJson: JsValue = Json.toJson(this)
}

object Customer {
  def getTestCustomer: Customer = {
    val id = "0010012404509"
    val title = "Ms"
    val firstName = "Tera"
    val surname = "Patrick"
    val dateOfBirth = "10/10/1976"
    val primaryTelephoneNumber = "07905576653"
    val primaryTelephoneType = "mobile"
    Customer(id, title, firstName, surname, dateOfBirth,
    primaryTelephoneNumber, primaryTelephoneType)
  }
}