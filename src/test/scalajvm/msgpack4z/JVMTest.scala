package msgpack4z

object Java06Spec extends SpecBase {
  override protected[this] def packer() = Msgpack06.defaultPacker()
  override protected[this] def unpacker(bytes: Array[Byte]) = Msgpack06.defaultUnpacker(bytes)
}

object JavaSpec extends SpecBase {
  override protected[this] def packer() = new MsgpackJavaPacker()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgpackJavaUnpacker.defaultUnpacker(bytes)
}
