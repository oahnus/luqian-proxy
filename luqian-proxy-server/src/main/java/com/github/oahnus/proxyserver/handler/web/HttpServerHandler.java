package com.github.oahnus.proxyserver.handler.web;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by oahnus on 2020-03-31
 * 14:53.
 */
@ChannelHandler.Sharable
@Slf4j
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final URL BASE_URL = HttpServerHandler.class.getResource("");
    private static final String WEB_ROOT = "WEB-INF";

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        HttpMethod method = request.method();
        String uri = request.uri();
        log.info("[HttpServerHandler].channelRead0 - Method: {}, Uri: {}", method, uri);
        String page = uri.equals("/") ? "index.html" : uri;

        File resource = getResource(page);

        // 404
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (!resource.exists()) {
            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_FOUND);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml;charset=utf-8;");

            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            context.write(response);
            ChannelFuture future = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
            context.close();
            return;
        }
        RandomAccessFile file = new RandomAccessFile(resource, "r");
        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
        String contextType = "text/html;";
        if(uri.endsWith(".css")){
            contextType = "text/css;";
        }else if(uri.endsWith(".js")){
            contextType = "text/javascript;";
        }else if(uri.toLowerCase().matches(".*\\.(jpg|png|gif|ico|jpeg)$")){
            String ext = uri.substring(uri.lastIndexOf(".") + 1);
            contextType = "image/" + ext;
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contextType + "charset=utf-8;");

        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        context.write(response);

        context.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));

        ChannelFuture future = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

        file.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel client = ctx.channel();
        log.info("Server Internal Error:"+ cause.getMessage());
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }

    private File getResource(String filename) throws URISyntaxException {
        String basePath = BASE_URL.toURI().toString();
        int start = basePath.indexOf("classes/");
        basePath = (basePath.substring(0,start) + "/" + "classes/").replaceAll("/+","/");

        String path = basePath + WEB_ROOT + "/" + filename;
        log.info("reqFile:" + filename);
        path = !path.contains("file:") ? path : path.substring(5);
        path = path.replaceAll("//", "/");
        return new File(path);
    }
}
