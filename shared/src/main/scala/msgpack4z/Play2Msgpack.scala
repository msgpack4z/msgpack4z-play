package msgpack4z

import play.api.libs.json._
import scalaz.{-\/, \/, \/-}

object Play2Msgpack {

  def jsValueCodec(options: PlayUnpackOptions): MsgpackCodec[JsValue] =
    new CodecPlay2JsValue(options)

  def jsArrayCodec(options: PlayUnpackOptions): MsgpackCodec[JsArray] =
    new CodecPlay2JsArray(options)

  def jsObjCodec(options: PlayUnpackOptions): MsgpackCodec[JsObject] =
    new CodecPlay2JsObject(options)

  def allCodec(options: PlayUnpackOptions): (MsgpackCodec[JsValue], MsgpackCodec[JsArray], MsgpackCodec[JsObject]) = (
    jsValueCodec(options),
    jsArrayCodec(options),
    jsObjCodec(options)
  )

  def jsObj2msgpack(packer: MsgPacker, obj: JsObject): Unit = {
    val fields = obj.value
    packer.packMapHeader(fields.size)
    fields.foreach { field =>
      packer.packString(field._1)
      json2msgpack(packer, field._2)
    }
    packer.mapEnd()
  }

  def jsArray2msgpack(packer: MsgPacker, array: JsArray): Unit = {
    packer.packArrayHeader(array.value.size)
    array.value.foreach { x =>
      json2msgpack(packer, x)
    }
    packer.arrayEnd()
  }

  def json2msgpack(packer: MsgPacker, json: JsValue): Unit = {
    json match {
      case JsNumber(v) =>
        if (v.isValidLong) {
          packer.packLong(v.longValue)
        } else {
          val f = v.floatValue
          if (f == v) {
            packer.packFloat(f)
          } else {
            packer.packDouble(v.doubleValue)
          }
        }
      case JsString(v) =>
        packer.packString(v)
      case JsBoolean(v) =>
        packer.packBoolean(v)
      case v @ JsObject(_) =>
        jsObj2msgpack(packer, v)
      case v @ JsArray(_) =>
        jsArray2msgpack(packer, v)
      case JsNull =>
        packer.packNil
    }
  }

  def msgpack2json(unpacker: MsgUnpacker, unpackOptions: PlayUnpackOptions): UnpackResult[JsValue] = {
    val result = Result.empty[JsValue]
    if (msgpack2json0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  def msgpack2jsObj(unpacker: MsgUnpacker, unpackOptions: PlayUnpackOptions): UnpackResult[JsObject] = {
    val result = Result.empty[JsObject]
    if (msgpack2jsObj0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  def msgpack2jsArray(unpacker: MsgUnpacker, unpackOptions: PlayUnpackOptions): UnpackResult[JsArray] = {
    val result = Result.empty[JsArray]
    if (msgpack2jsArray0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  private[this] val JsTrue = JsBoolean(true)
  private[this] val JsFalse = JsBoolean(false)

  private[this] final case class Result[A](
    var value: A, var error: UnpackError
  )
  private[this] object Result {
    def fromEither[A](e: UnpackError \/ A, result: Result[A]): Boolean = e match{
      case \/-(r) =>
        result.value = r
        true
      case -\/(l) =>
        result.error = l
        false
    }

    def empty[A >: Null <: JsValue]: Result[A] = Result[A](null, null)
  }

  private[this] def msgpack2jsObj0(unpacker: MsgUnpacker, result: Result[JsObject], unpackOptions: PlayUnpackOptions): Boolean = {
    val size = unpacker.unpackMapHeader()
    val fields = new Array[(String, JsValue)](size)
    var i = 0
    val mapElem = Result.empty[JsValue]
    var success = true

    def process(key: String): Unit = {
     if (msgpack2json0(unpacker, mapElem, unpackOptions)) {
       fields(i) = (key, mapElem.value)
       i += 1
     } else {
       result.error = mapElem.error
       success = false
     }
    }

    while (i < size && success) {
      val tpe = unpacker.nextType()
      if(tpe == MsgType.STRING) {
        process(unpacker.unpackString())
      }else{
        unpackOptions.nonStringKey(tpe, unpacker) match {
          case Some(key) =>
            process(key)
          case None =>
            success = false
            result.error = Other("not string key")
        }
      }
    }
    unpacker.mapEnd()
    if (success) {
      result.value = JsObject(fields.toVector)
    }
    success
  }

  private[this] def msgpack2jsArray0(unpacker: MsgUnpacker, result: Result[JsArray], unpackOptions: PlayUnpackOptions): Boolean = {
    val size = unpacker.unpackArrayHeader()
    val array = new Array[JsValue](size)
    var i = 0
    val arrayElem = Result[JsValue](null, null)
    var success = true
    while (i < size && success) {
      if (msgpack2json0(unpacker, arrayElem, unpackOptions)) {
        array(i) = arrayElem.value
        i += 1
      } else {
        result.error = arrayElem.error
        success = false
      }
    }
    unpacker.arrayEnd()
    if (success) {
      result.value = JsArray(array.toVector)
    }
    success
  }


  private def msgpack2json0(unpacker: MsgUnpacker, result: Result[JsValue], unpackOptions: PlayUnpackOptions): Boolean = {
    unpacker.nextType match {
      case MsgType.NIL =>
        unpacker.unpackNil()
        result.value = JsNull
        true
      case MsgType.BOOLEAN =>
        if (unpacker.unpackBoolean()) {
          result.value = JsTrue
        } else {
          result.value = JsFalse
        }
        true
      case MsgType.INTEGER =>
        result.value = JsNumber(new java.math.BigDecimal(unpacker.unpackBigInteger()))
        true
      case MsgType.FLOAT =>
        val f = unpacker.unpackDouble()
        try{
          result.value = JsNumber(BigDecimal.valueOf(f))
          true
        }catch{
          case _: NumberFormatException if f.isPosInfinity =>
            Result.fromEither(unpackOptions.positiveInf, result)
          case _: NumberFormatException if f.isNegInfinity =>
            Result.fromEither(unpackOptions.negativeInf, result)
          case _: NumberFormatException if java.lang.Double.isNaN(f) =>
            Result.fromEither(unpackOptions.nan, result)
        }
      case MsgType.STRING =>
        result.value = JsString(unpacker.unpackString())
        true
      case MsgType.ARRAY =>
        val result0 = Result.empty[JsArray]
        val r = msgpack2jsArray0(unpacker, result0, unpackOptions)
        result.error = result0.error
        result.value = result0.value
        r
      case MsgType.MAP =>
        val result0 = Result.empty[JsObject]
        val r = msgpack2jsObj0(unpacker, result0, unpackOptions)
        result.error = result0.error
        result.value = result0.value
        r
      case MsgType.BINARY =>
        Result.fromEither(unpackOptions.binary(unpacker), result)
      case MsgType.EXTENSION =>
        Result.fromEither(unpackOptions.extension(unpacker), result)
    }
  }
}


private final class CodecPlay2JsArray(unpackOptions: PlayUnpackOptions) extends MsgpackCodecConstant[JsArray](
  (packer, js) => Play2Msgpack.jsArray2msgpack(packer, js),
  unpacker => Play2Msgpack.msgpack2jsArray(unpacker, unpackOptions)
)

private final class CodecPlay2JsValue(unpackOptions: PlayUnpackOptions) extends MsgpackCodecConstant[JsValue](
  (packer, js) => Play2Msgpack.json2msgpack(packer, js),
  unpacker => Play2Msgpack.msgpack2json(unpacker, unpackOptions)
)

private final class CodecPlay2JsObject(unpackOptions: PlayUnpackOptions) extends MsgpackCodecConstant[JsObject](
  (packer, js) => Play2Msgpack.jsObj2msgpack(packer, js),
  unpacker => Play2Msgpack.msgpack2jsObj(unpacker, unpackOptions)
)
