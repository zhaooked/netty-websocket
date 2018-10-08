package com.lance.net.server.common;

import com.lance.net.server.module.UserInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private Logger loger = LogManager.getLogger();
	private final String webUri;

	public HttpRequestHandler(String webUri) {
		this.webUri = webUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		loger.info("===========> {}, {}", webUri, request.uri());
		
		String uri = StringUtils.substringBefore(request.uri(), "?");
		if(webUri.equalsIgnoreCase(uri)) {//获取webSocket参数
			QueryStringDecoder query = new QueryStringDecoder(request.uri());
			Map<String, List<String>> map = query.parameters();
			List<String> tokens = map.get("token");
			
			//根据参数保存当前登录对象, 并把该token加入到channel中
			if(tokens != null && !tokens.isEmpty()) {
				Channel channel = ctx.channel();
				String token = tokens.get(0);
				ChatConstants.addOnlines(token, new UserInfo(token),channel);
				channel.attr(ChatConstants.CHANNEL_TOKEN_KEY).getAndSet(token);
			}
			
			request.setUri(uri);
			ctx.fireChannelRead(request.retain());
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
