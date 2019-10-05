package PrivateHome.Devices

import PrivateHome.Devices.MHz.mhzSwitch

import scala.xml._

/**
 * The general Switch class
 * @param setupID an unique Base64 ID
 * @param setupKeepStatus toggles if the Switch should save State over program restart (failure)
 */
abstract class Switch(private val setupID:String, setupKeepStatus:Boolean) {


  if (setupID.length != 5) throw new IllegalArgumentException("""Length of ID is not 5""")
  if (!setupID.matches("[-_a-zA-Z0-9]{5}")) throw new IllegalArgumentException("""ID Contains not Allowed Characters""")
  //TODO: Control that the ID isn't used yet

  private var _status = false

  def on():Unit
  def off():Unit

  /**
   * Sets the Status of the Switch only changes State after receiving a Confirmation
   * @param boolean The State the Switch should change to
   */
  def Status(boolean: Boolean): Unit = {
    _status = boolean
    if (setupKeepStatus) {} //TODO: Trigger Status save
  }
  def Status_():Boolean = _status
  def id(): String = setupID

  def toXml:Node

}

object Switch {
  def apply(data:Node): Switch = {
    val switchType = (data \ "type").head.text
    println(switchType)
    switchType match {
      case "433MHz" => val systemCode = (data \ "systemCode").head.text
        val unitCode = (data \ "unitCode").head.text
        val KeepStatus = (data \ "keepStatus").head.text.toBoolean
        val ID = (data \ "id").head.text
        mhzSwitch(ID, KeepStatus, systemCode, unitCode);
      case _ => throw new IllegalArgumentException("Wrong Switch Type")
    }
  }
}