package org.mockserver.callback;

import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.HttpStateHandler;

import java.util.UUID;

import static com.google.common.net.HttpHeaders.HOST;
import static org.mockserver.exception.ExceptionHandler.shouldNotIgnoreException;

/**
 * @author jamesdbloom
 */
@ChannelHandler.Sharable
public class CallbackWebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<Boolean> CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET = AttributeKey.valueOf("CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET");
    private static final String UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI = "/_mockserver_callback_websocket";

    private WebSocketServerHandshaker handshaker;
    private WebSocketClientRegistry webSocketClientRegistry;
    private final MockServerLogger mockServerLogger;

    public CallbackWebSocketServerHandler(HttpStateHandler httpStateHandler) {
        webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
        mockServerLogger = httpStateHandler.getMockServerLogger();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean release = true;
        try {
            if (msg instanceof FullHttpRequest && ((FullHttpRequest) msg).uri().equals(UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI)) {
                upgradeChannel(ctx, (FullHttpRequest) msg);
                ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).set(true);
            } else if (ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).get() != null &&
                ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).get() &&
                msg instanceof WebSocketFrame) {
                handleWebSocketFrame(ctx, (WebSocketFrame) msg);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void upgradeChannel(final ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        handshaker = new WebSocketServerHandshakerFactory(
            "ws://" + httpRequest.headers().get(HOST) + UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI,
            null,
            true,
            Integer.MAX_VALUE
        ).newHandshaker(httpRequest);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            final String clientId = UUID.randomUUID().toString();
            handshaker.handshake(
                ctx.channel(),
                httpRequest,
                new DefaultHttpHeaders().add("X-CLIENT-REGISTRATION-ID", clientId),
                ctx.channel().newPromise()
            ).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    webSocketClientRegistry.registerClient(clientId, ctx);
                    // TODO(jamesdbloom) remove mockserver codec and handler
                }
            });
        }
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        } else if (frame instanceof TextWebSocketFrame) {
            webSocketClientRegistry.receivedTextWebSocketFrame(((TextWebSocketFrame) frame));
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
        } else {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (shouldNotIgnoreException(cause)) {
            mockServerLogger.error("web socket server caught exception", cause);
        }
        ctx.close();
    }

}
