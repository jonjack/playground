package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import models.Customer

class CustomerAPI extends Controller {
  
  def customersv1 = Action {
    val customer = Customer.getTestCustomer
    val jsonData: JsValue = customer.asJson
    val json: JsValue = Json.obj("status" -> "SUCCESS", "version" ->
    "v1.0", "data" -> jsonData, "errors" -> "","meta" -> "")
    Ok(json)
  }
  
  implicit val customersV2 = new Writes[Customer] {
    def writes(customer: Customer) = Json.obj(
    "id" -> customer.id,
    "title" -> customer.firstName,
    "dob" -> customer.dateOfBirth)
  }
  
  def customersv2 = Action {
    val customer = Customer.getTestCustomer
    val jsonData = Json.toJson(customer)
    val json: JsValue = Json.obj("status" -> "SUCCESS", "version" ->
    "v2.0", "data" -> jsonData, "errors" -> "","meta" -> "")
    Ok(json)
  }
  
}