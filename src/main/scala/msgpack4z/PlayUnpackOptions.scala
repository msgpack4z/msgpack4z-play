package msgpack4z

import java.util.Base64
import msgpack4z.PlayUnpackOptions.NonStringKeyHandler
import play.api.libs.json._
import scalaz.\/-

final case class PlayUnpackOptions(
  extension: Unpacker[JsValue],
  binary: Unpacker[JsValue],
  positiveInf: UnpackResult[JsValue],
  negativeInf: UnpackResult[JsValue],
  nan: UnpackResult[JsValue],
  nonStringKey: NonStringKeyHandler
)

object PlayUnpackOptions {
  val binaryToNumberArray: Binary => JsValue = { bytes =>
    JsArray(bytes.value.map(x => JsNumber(x: Int)))
  }

  val binaryToNumberArrayUnpacker: Unpacker[JsValue] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  val binaryToBase64JsString: Binary => JsValue = { bytes =>
    JsString(Base64.getEncoder.encodeToString(bytes.value))
  }

  val binaryToBase64JsStringUnpacker: Unpacker[JsValue] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  type NonStringKeyHandler = (MsgType, MsgUnpacker) => Option[String]

  val extUnpacker: Unpacker[JsValue] = { unpacker =>
    val header = unpacker.unpackExtTypeHeader()
    val data = unpacker.readPayload(header.getLength)
    val result = Json.obj(
      ("type", JsNumber(header.getType: Int)),
      ("data", JsString(Base64.getEncoder.encodeToString(data)))
    )
    \/-(result)
  }

  val default: PlayUnpackOptions = PlayUnpackOptions(
    extUnpacker,
    binaryToNumberArrayUnpacker,
    \/-(JsNull),
    \/-(JsNull),
    \/-(JsNull),
    { case (tpe, unpacker) =>
      PartialFunction.condOpt(tpe) {
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
