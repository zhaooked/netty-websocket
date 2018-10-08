package com.lance.net.server.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lance.net.server.module.ChatMessage;
import com.lance.net.server.module.UserInfo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>{
	private Logger loger = LogManager.getLogger();
	private final ChannelGroup group;
	
	public TextWebSocketFrameHandler(ChannelGroup group) {
		this.group = group;
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		loger.info("Event====>{}", evt);
		
		if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
			ctx.pipeline().remove(HttpRequestHandler.class);
			
			//加入当前, 上线人员推送前端，显示用户列表中去
			Channel channel = ctx.channel();
            String token = channel.attr(ChatConstants.CHANNEL_TOKEN_KEY).get();
            UserInfo online = ChatConstants.onlines.get(token);
			Map<String,Object> re = new HashMap<>();
			re.put("aid",online.getPhone());
			re.put("nickname",online.getPhone());
			re.put("avatar",online.getHeadImg().replace("resources","static"));
			re.put("type","self_info");
			channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(re,SerializerFeature.DisableCircularReferenceDetect)));
            Map<String,Object> all = new HashMap<>();
            all.put("type","friend_list");
			all.put("list",ChatConstants.onlines.values().stream().map(u->{
			    Map<String,Object> m = new HashMap<>();
			    m.put("is_online",1);
			    m.put("aid",u.getPhone());
			    m.put("id",u.getId());
			    m.put("nickname",u.getPhone());
			    m.put("session_id",u.getPhone());
			    m.put("avatar",u.getHeadImg().replace("resources","static"));
			    return m;
            }).collect(Collectors.toList()));
			channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(all,SerializerFeature.DisableCircularReferenceDetect)));
			re.put("type","friend_added");
			group.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(re,SerializerFeature.DisableCircularReferenceDetect)));
			group.add(channel);
		}else {
			super.userEventTriggered(ctx, evt);
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
		Channel channel = ctx.channel();
		String token = channel.attr(ChatConstants.CHANNEL_TOKEN_KEY).get();
		UserInfo from = ChatConstants.onlines.get(token);
		if(from == null) {
			group.writeAndFlush("OK");
		}else {
			ChatMessage message = new ChatMessage(from, msg.text());
			loger.info("Bordcast ... from :" + token);
			group.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message,SerializerFeature.DisableCircularReferenceDetect)));
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		loger.info("Current channel channelInactive");
		offlines(ctx);
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		loger.info("Current channel handlerRemoved");
		offlines(ctx);
	}
	
	private void offlines(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		String token = channel.attr(ChatConstants.CHANNEL_TOKEN_KEY).get();
		ChatConstants.removeOnlines(token);
		
		group.remove(channel);
		ctx.close();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		loger.error("=====> {}", cause.getMessage());
		offlines(ctx);
	}
}
