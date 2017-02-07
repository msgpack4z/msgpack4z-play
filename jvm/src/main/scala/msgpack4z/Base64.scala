package msgpack4z

private[msgpack4z] object Base64 {
  def encode(bytes: Array[Byte]): String =
    java.util.Base64.getEncoder.encodeToString(bytes)
}
