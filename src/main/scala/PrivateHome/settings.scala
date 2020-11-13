package PrivateHome


import java.io.{File, FileNotFoundException, PrintWriter}

import PrivateHome.UI.uiControl.formats
import org.json4s.JsonAST
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.io.Source


//ToDo: make settings for an config file
case object settings {
  var websocket: http = new http(port = 2888, "ws")
  var http: http = new http(2000, "Website") //ToDo: change to 80 in produktion
  var database = new database(userName = "user", password = "pass", "./daten/Devices")
  var mqtt = mqttBroker("localhost", 1500)

  load()

  def load(): Unit = {
    try {
      val fSource = Source.fromFile("settings.json")
    }
    catch {
      case e: FileNotFoundException => println("settings.json doesn't exist. Saving standard to create a new one.")
        save()
      case e: Throwable => throw e
    }

    val fSource = Source.fromFile("settings.json")
    var ftext: String = ""
    for (line <- fSource.getLines())
      ftext = ftext + line
    val jsonobj = parse(ftext)
    val web = jsonobj \ "websocket"
    websocket = web.extract[http]

    val httpJson = jsonobj \ "http"
    http = httpJson.extract[http]

    val databaseJson = jsonobj \ "database"
    database = databaseJson.extract[database]

    val mqttJson = jsonobj \ "mqtt"
    mqtt = mqttJson.extract[mqttBroker]


  }

  def save(): Unit = {
    val writer = new PrintWriter(new File("settings.json"))
    writer.write(pretty(render(("websocket" -> websocket.serialized) ~ ("http" -> http.serialized) ~ ("database" -> database.serialized) ~ ("mqtt" -> mqtt.serialized))))
    writer.close()
  }

  trait settings

}

case class http(var port: Int, var path: String = "") extends setting {
  if (port < 0 || port > 0xffff) throw new IllegalArgumentException("Argument Port out of bound must be between 0x0 and 0xffff=65536")

  def serialized: JsonAST.JObject = {
    ("port" -> port) ~ ("path" -> path)
  }
}

case class database(var userName: String, var password: String, var path: String) extends setting {
  override def serialized: JsonAST.JObject = ("userName" -> userName) ~ ("password" -> password) ~ ("path" -> path)

}

case class mqttBroker(var url: String, var port: Int) extends setting {
  if (port < 0 || port > 0xffff) throw new IllegalArgumentException("Argument Port aut of bound must be between 0x0 and 0xffff=65536")

  override def serialized: JsonAST.JObject = ("url" -> url) ~ ("port" -> port)
}

abstract class setting {
  def serialized: JsonAST.JObject
}
