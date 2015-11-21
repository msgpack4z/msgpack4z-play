package msgpack4z

sealed abstract class UndefinedHandler(private[msgpack4z] val f: MsgPacker => Unit) extends Product with Serializable

object UndefinedHandler {
  case object ConvertNil extends UndefinedHandler(_.packNil())
  case object Ignore extends UndefinedHandler(Function.const(()))
  final case class ThrowError(error: () => Nothing) extends UndefinedHandler(_ => error())
  final case class ExecuteFunction(func: MsgPacker => Unit) extends UndefinedHandler(func)
  val ThrowSysError: ThrowError = ThrowError(() => sys.error("JsUndefined"))
}
