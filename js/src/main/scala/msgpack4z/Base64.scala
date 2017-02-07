package msgpack4z

import com.github.marklister.base64.Base64._

private[msgpack4z] object Base64 {
  def encode(bytes: Array[Byte]): String =
    bytes.toBase64
}
