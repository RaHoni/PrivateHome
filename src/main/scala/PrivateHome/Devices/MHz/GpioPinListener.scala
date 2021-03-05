package PrivateHome.Devices.MHz

import java.util.NoSuchElementException

import PrivateHome.data
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}

class GpioPinListener extends GpioPinListenerDigital {
  private val nSeperationlimit = 4600
  private val nReceiveTolerance = 60
  private val rcSwitchMaxChanges = 67
  private val timings: Array[Int] = new Array[Int](rcSwitchMaxChanges)
  private var changeCount = 0
  private var lastTime: Long = 0
  private var repeatCount = 0

  /**
   * The Interrupt handler for receiving 433MHz Codes
   *
   * @param event An object containing all information off the StateChangeEvent
   */
  override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
    val time = System.nanoTime() / 1000
    val signalDuration: Int = (time - lastTime).toInt

    if (signalDuration > nSeperationlimit) {
      // A long stretch without signal level change occurred. This could
      // be the gap between two transmission.
      if (diffAbs(signalDuration, timings(0)) < 200) {
        // This long signal is close in length to the long signal which
        // started the previously recorded timings; this suggests that
        // it may indeed by a a gap between two transmissions (we assume
        // here that a sender will send the signal multiple times,
        // with roughly the same gap between them).
        repeatCount += 1
        if (repeatCount == 2) {
          receiveProtocol(changeCount)
          repeatCount = 0
        }

      }
      changeCount = 0
    }

    if (changeCount >= rcSwitchMaxChanges) {
      changeCount = 0
      repeatCount = 0
    }
    timings(changeCount) = signalDuration
    changeCount += 1
    lastTime = time
  }

  /**
   * Interprets the timing Array as a Code of 0/1. And Calls the InterpretCode function
   *
   * @param pChangeCount the Number how often the signal changed state
   * @return True if the Interpretation has succeeded
   */
  private def receiveProtocol(pChangeCount: Int): Boolean = {
    if (pChangeCount > 7) {
      var code: Long = 0
      val delay = timings(0) / Protocol.sync.low
      val delayTolerance = delay * nReceiveTolerance / 100

      for (i <- 1 until pChangeCount - 1 by 2) {
        code <<= 1
        if (diffAbs(timings(i), delay * Protocol.zero.high) < delayTolerance &&
          diffAbs(timings(i + 1), delay * Protocol.zero.low) < delayTolerance) {
          //Zero
        } else if (diffAbs(timings(i), delay * Protocol.one.high) < delayTolerance &&
          diffAbs(timings(i + 1), delay * Protocol.one.low) < delayTolerance) {
          //One
          code |= 1
        } else {
          //Failed

        }
      }
      interpretCode(code, (pChangeCount - 1) / 2)
    }
    else false


  }

  /**
   * Gives the absolute difference
   *
   * @param a First number
   * @param b Second number
   * @return The result of the Calculation
   */
  private def diffAbs(a: Int, b: Int): Int = math.abs(a - b)

  /**
   * Interprets an Binary code in to the systemCode, unitCode and Command
   *
   * @param pCode    The Binary code to interpret like generated by the InterpretProtocol function
   * @param bitCount The Length of the Code needed because its not the length of the Long
   * @return True if succeeded
   */
  private def interpretCode(pCode: Long, bitCount: Int): Boolean = {
    var code = pCode
    var commandCode = ""


    val command = (code & 15) == codec.command(true)
    code >>= 4
    for (_ <- 0 until bitCount - 4 by 2) {

      if ((code & 3) == codec.code('0')) {
        commandCode += "0"
        code >>= 2
      }
      else if ((code & 3) == codec.code('1')) {
        commandCode += "1"
        code >>= 2
      }
      else {
        println("error" + (code & 3))
        return false

      }


    }

    commandCode = commandCode.reverse

    val systemCode = commandCode.substring(0, 5)
    val unitCode = commandCode.substring(5)

    println(s"""An: $command; SystemCode: $systemCode UnitCode: $unitCode""")

    try {
      val ID = data.mhzId(commandCode)
      data.devices(ID).status = if (command) 1 else 0
    } catch {
      case _:NoSuchElementException => ;
      case unknown:Throwable => throw unknown
    }


    true
  }

}
