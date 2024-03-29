package msgpack4z

import scalaprops._
import play.api.libs.json._
import scalaz.{-\/, \/-, Equal}

abstract class SpecBase extends Scalaprops {

  private[this] implicit val stringGen: Gen[String] = Gen.alphaNumString

  private val bigDecimalGen: Gen[BigDecimal] =
    Gen[Long].map(BigDecimal(_))

  private val jsValuePrimitivesArb: Gen[JsValue] =
    Gen.oneOf(
      Gen.value(JsNull),
      Gen[Boolean].map(JsBoolean(_)),
      bigDecimalGen.map(JsNumber.apply),
      Gen[String].map(JsString.apply)
    )

  private val jsObjectArb1: Gen[JsObject] =
    Gen
      .listOfN(
        10,
        Gen.tuple2(
          Gen[String],
          jsValuePrimitivesArb
        )
      )
      .map(pairs => JsObject(pairs.toMap.toSeq))

  private val jsArrayArb1: Gen[JsArray] =
    Gen.listOfN(10, jsValuePrimitivesArb).map(JsArray(_))

  implicit val jsValueArb: Gen[JsValue] =
    Gen.oneOf(
      jsValuePrimitivesArb,
      jsObjectArb1.map(identity),
      jsArrayArb1.map(identity)
    )

  implicit val jsObjectArb: Gen[JsObject] =
    Gen
      .listOfN(
        10,
        Gen.tuple2(Gen[String], jsValueArb)
      )
      .map(pairs => JsObject(pairs.toMap.toSeq))

  implicit val jsArrayArb: Gen[JsArray] =
    Gen.listOfN(10, jsValueArb).map(JsArray(_))

  protected[this] def packer(): MsgPacker
  protected[this] def unpacker(bytes: Array[Byte]): MsgUnpacker

  private def checkRoundTripBytes[A](a: A)(implicit A: MsgpackCodec[A], E: Equal[A]): Boolean =
    A.roundtripz(a, packer(), unpacker _) match {
      case None =>
        true
      case Some(\/-(b)) =>
        println("fail roundtrip bytes " + a + " " + b)
        false
      case Some(-\/(e)) =>
        println(e)
        false
    }

  val test = {
    implicit val codecInstance: MsgpackCodec[JsValue] = Play2Msgpack.jsValueCodec(
      PlayUnpackOptions.default
    )
    implicit val equalInstance: Equal[JsValue] = Equal.equalA[JsValue]

    Property.forAll { (json: JsValue) =>
      checkRoundTripBytes(json)
    }
  }
}

object NativeSpec extends SpecBase {
  override protected[this] def packer() = MsgOutBuffer.create()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgInBuffer(bytes)
}
