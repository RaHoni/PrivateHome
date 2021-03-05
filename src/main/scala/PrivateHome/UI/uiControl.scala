package PrivateHome.UI

import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.data
import PrivateHome.data.devices
import org.json4s.JsonAST.{JField, JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}

object uiControl {

  implicit val formats: Formats = Serialization.formats(NoTypeHints) + new switchSerializer

  def receiveCommand(command: Command): Any = {
    try {
      command match {
        case c: commandOn => devices(c.id).on(c.percentFloat)
          true
        case c: commandOff => devices(c.id).off()
          true
        case _: commandGetDevices =>
          var devicesJson: List[JValue] = List()
          for (device <- data.devices) {

            devicesJson = devicesJson.concat(List(JsonMethods.parse(write(device._2))))
          }
          JObject(JField("devices", devicesJson)) // because we don't use the "~" we must lift it to JSON that is why we use JObject(JField()) instead an simple "devices" -> devicesJSon.
        case c: commandAddDevice => data.addDevice(Switch(c)); true
        case c: commandGetDevice => JsonMethods.parse(write(data.getDevice(c.id)))

        case c: commandAddUserBase64 => data.addUser(c.userName, c.passHash)
          true
        case c: commandRecreateDatabase => data.create(true); true
        case _: commandSafeCreateDatabase => data.create(); true
        case c: commandUpdateDevice => data.updateDevice(c.oldId,Switch(c)); true
      }
    } catch {
      case exception: Exception => exception
    }
  }
}
