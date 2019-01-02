package org.jboss.shamrock.vertx.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.compression.ZlibDecoder;
import io.netty.handler.codec.compression.ZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * This substitution avoid having jcraft zlib added to the build
 */
@TargetClass(className = "io.netty.handler.codec.compression.ZlibCodecFactory")
final class Target_io_netty_handler_codec_compression_ZlibCodecFactory {

  @Substitute
  public static ZlibEncoder newZlibEncoder(int compressionLevel) {
    return new JdkZlibEncoder(compressionLevel);
  }

  @Substitute
  public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper) {
    return new JdkZlibEncoder(wrapper);
  }

  @Substitute
  public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper, int compressionLevel) {
    return new JdkZlibEncoder(wrapper, compressionLevel);
  }

  @Substitute
  public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper, int compressionLevel, int windowBits, int memLevel) {
    return new JdkZlibEncoder(wrapper, compressionLevel);
  }

  @Substitute
  public static ZlibEncoder newZlibEncoder(byte[] dictionary) {
    return new JdkZlibEncoder(dictionary);
  }

  @Substitute
  public static ZlibEncoder newZlibEncoder(int compressionLevel, byte[] dictionary) {
    return new JdkZlibEncoder(compressionLevel, dictionary);
  }

  @Substitute
  public static ZlibEncoder newZlibEncoder(int compressionLevel, int windowBits, int memLevel, byte[] dictionary) {
    return new JdkZlibEncoder(compressionLevel, dictionary);
  }

  @Substitute
  public static ZlibDecoder newZlibDecoder() {
    return new JdkZlibDecoder();
  }

  @Substitute
  public static ZlibDecoder newZlibDecoder(ZlibWrapper wrapper) {
    return new JdkZlibDecoder(wrapper);
  }

  @Substitute
  public static ZlibDecoder newZlibDecoder(byte[] dictionary) {
    return new JdkZlibDecoder(dictionary);
  }
}

class ZLibSubstitutions {

}
