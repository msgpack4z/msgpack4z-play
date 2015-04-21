package msgpack6z

import msgpack4z._
import org.scalacheck.{Gen, Arbitrary, Prop, Properties}
import play.api.libs.json._
import scala.util.control.NonFatal
import scalaz.{-\/, Equal, \/-}

abstract class SpecBase(name: String) extends Properties(name){

  private val bigDecimalGen: Gen[BigDecimal] =
    Gen.choose(Long.MinValue, Long.MaxValue).map(BigDecimal(_))

  private val jsValuePrimitivesArb: Arbitrary[JsValue] =
    Arbitrary(Gen.oneOf(
      Gen.const(JsNull),
      gen[Boolean].map(JsBoolean),
      bigDecimalGen.map(JsNumber),
      gen[String].map(JsString)
    ))

  private val jsObjectArb1: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(
          arb[String], jsValuePrimitivesArb
        ).arbitrary
      ).map(pairs => JsObject(pairs.toMap.toSeq))
    ))

  private val jsArrayArb1: Arbitrary[JsArray] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(n, jsValuePrimitivesArb.arbitrary).map(JsArray)
    ))

  implicit val jsValueArb: Arbitrary[JsValue] =
    Arbitrary(Gen.oneOf(
      jsValuePrimitivesArb.arbitrary,
      jsObjectArb1.arbitrary,
      jsArrayArb1.arbitrary
    ))

  implicit val jsObjectArb: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(arb[String], jsValueArb).arbitrary
      ).map(pairs => JsObject(pairs.toMap.toSeq))
    ))

  implicit val jsArrayArb: Arbitrary[JsArray] =
    Arbitrary(Gen.choose(0, 6).flatMap(n =>
      Gen.listOfN(n, jsValueArb.arbitrary).map(JsArray)
    ))

  final def gen[A: Arbitrary]: Gen[A] =
    implicitly[Arbitrary[A]].arbitrary

  final def arb[A: Arbitrary]: Arbitrary[A] =
    implicitly[Arbitrary[A]]

  protected[this] def packer(): MsgPacker
  protected[this] def unpacker(bytes: Array[Byte]): MsgUnpacker

  private def checkRoundTripBytes[A](a: A)(implicit A: MsgpackCodec[A], G: Arbitrary[A], E: Equal[A]): Boolean =
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

  property("play-json") = {
    implicit val codecInstance = Play2Msgpack.jsValueCodec(
      PlayUnpackOptions.default
    )
    implicit val equalInstance = Equal.equalA[JsValue]

    Prop.forAll{ json: JsValue =>
      Prop.secure{
        try {
          checkRoundTripBytes(json)
        }catch{
          case NonFatal(e) =>
            println(json)
            println(e.getStackTrace.map("\tat " + _).mkString("\n" + e.toString + "\n","\n", "\n"))
            throw e
        }
      }
    }
  }
}

object Java06Spec extends SpecBase("java06"){
  override protected[this] def packer() = Msgpack06.defaultPacker()
  override protected[this] def unpacker(bytes: Array[Byte]) = Msgpack06.defaultUnpacker(bytes)
}

object Java07Spec extends SpecBase("java07"){
  override protected[this] def packer() = new Msgpack07Packer()
  override protected[this] def unpacker(bytes: Array[Byte]) = Msgpack07Unpacker.defaultUnpacker(bytes)
}

object NativeSpec extends SpecBase("native"){
  override protected[this] def packer() = MsgOutBuffer.create()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgInBuffer(bytes)
}
