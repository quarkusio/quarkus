package io.quarkus.netty.runtime;

import io.netty.channel.ChannelHandlerAdapter;

/**
 * Custom marker interface used to do faster {@link ChannelHandlerAdapter#isSharable()} checks as an interface instanceof
 * is much faster than looking up an annotation
 */
public interface NettySharable {
}
