package msgpack4z

import msgpack4z.PlayUnpackOptions.NonStringKeyHandler
import play.api.libs.json._
import scalaz.{\/-, -\/}

final case class PlayUnpackOptions(
  extended: Unpacker[JsValue],
  binary: Unpacker[JsValue],
  positiveInf: UnpackResult[JsValue],
  negativeInf: UnpackResult[JsValue],
  nan: UnpackResult[JsValue],
  nonStringKey: NonStringKeyHandler
)

object PlayUnpackOptions {
  val binaryToNumberArray: Binary => JsValue = { bytes =>
    JsArray(bytes.value.map(JsNumber(_)))
  }

  val binaryToNumberArrayUnpacker: Unpacker[JsValue] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  type NonStringKeyHandler = (MsgType, MsgUnpacker) => Option[String]

  val default: PlayUnpackOptions = PlayUnpackOptions(
    _ => -\/(Err(new Exception("does not support extended type"))),
    binaryToNumberArrayUnpacker,
    \/-(JsNull),
    \/-(JsNull),
    \/-(JsNull),
    {case (tpe, unpacker) =>
      PartialFunction.condOpt(tpe){
        case MsgType.NIL =>
          "null"
        case MsgType.BOOLEAN =>
          unpacker.unpackBoolean().toString
        case MsgType.INTEGER =>
          unpacker.unpackBigInteger().toString
        case MsgType.FLOAT =>
          unpacker.unpackDouble().toString
        case MsgType.STRING =>
          unpacker.unpackString()
      }
    }
  )

}