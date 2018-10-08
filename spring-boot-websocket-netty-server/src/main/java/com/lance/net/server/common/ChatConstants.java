package com.lance.net.server.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.lance.net.server.module.UserInfo;

import io.netty.util.AttributeKey;

public class ChatConstants {
    public static final AttributeKey<String> CHANNEL_TOKEN_KEY = AttributeKey.valueOf("netty.channel.token");  
	/**用来保存当前在线人员*/
	public static Map<String, UserInfo> onlines = new ConcurrentHashMap<>();

	public static Map<String,Channel> onlineChannel = new ConcurrentHashMap<>();

	/**
	 * 存放Group Channel
	 */
	public static final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
	
	public static void addOnlines(String token, UserInfo val,Channel channel) {
		onlines.putIfAbsent(token, val);
		onlineChannel.putIfAbsent(token,channel);
	}
	
	public static void removeOnlines(String token) {
		if(StringUtils.isNotBlank(token) && onlines.containsKey(token)){
			onlines.remove(token);
			onlineChannel.remove(token);
		}
	}
	
	private static char[]prefix = {'A','B','C','D','E','F','G','H','J','K','L','M','N','P','Q','R','S','T','U','V','W','X','Y'};
	private static int[]imgPrefix = {1,2,3,4,5,6,7,8,9,10,11};
	
	public static String headImg() {
		int index = RandomUtils.nextInt(0, imgPrefix.length);
		return "/resources/img/head/"+imgPrefix[index]+".jpg";
	}
	
	public static String code() {
		int index = RandomUtils.nextInt(0, prefix.length);
		char prf = prefix[index];
		String len = (onlines.size()+1)+"";
		if(len.length() < 4) {
			len = StringUtils.leftPad(len, 4, '0');
		}
		return prf+len;
	}
}
