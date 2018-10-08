package com.lance.net.server.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lance.net.server.common.ChatConstants;
import com.lance.net.server.module.UserInfo;
import com.lance.net.server.oss.OSSFactory;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/chat")
public class ChatController {


    // 跳转到交谈聊天页面
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public String talk(String token, Model model) {
        model.addAttribute("token", token);
        return "chat.jsp";
    }

    @ResponseBody
    @RequestMapping(value = "users", method = RequestMethod.GET, produces = {"application/json; charset=UTF-8", "text/plain"})
    public String users(String token) {
        Map<String, UserInfo> onlines = ChatConstants.onlines;
        UserInfo cur = onlines.get(token);

        Map<String, Object> map = new HashMap<>(2);
        map.put("curName", cur != null ? cur.getCode() : "");
        map.put("users", onlines);
        return JSON.toJSONString(map);
    }

    @ResponseBody
    @RequestMapping(value = "friends")
    public String friends() {
        Map<String, UserInfo> onlines = ChatConstants.onlines;
        Map<String, Object> map = new HashMap<>();
        map.put("friend_list", onlines.values());
        return JSON.toJSONString(map);
    }

    @ResponseBody
    @RequestMapping(value = "info")
    public String info(String sessionId) {
        UserInfo info = ChatConstants.onlines.get(sessionId);
        if (info == null) {
            return "";
        } else {
            Map<String, Object> map = new HashMap<>();
            map.put("aid", info.getPhone());
            map.put("avatar", info.getHeadImg().replace("resources", "static"));
            return JSON.toJSONString(map);
        }
    }

    @ResponseBody
    @RequestMapping(value = "time")
    public String info() {
       return String.valueOf(new Date().getTime());
    }

    @ResponseBody
    @RequestMapping(value = "sendFriend")
    public String sendData(@RequestParam(value = "message") String message, @RequestParam(value = "fromSessionId") String fromSessionId, @RequestParam(value = "toSessionId") String toSessionId) {
        UserInfo fromUserInfo = ChatConstants.onlines.get(fromSessionId);
        UserInfo toUserInfo = ChatConstants.onlines.get(toSessionId);
        Channel channel = ChatConstants.onlineChannel.get(toSessionId);
        Map<String, Object> re = new HashMap<>();
        if (channel != null) {
            String token = channel.attr(ChatConstants.CHANNEL_TOKEN_KEY).get();
            System.out.println(token);
            Map<String, Object> to = new HashMap<>();
            to.put(toSessionId, toUserInfo);
            to.put("type", "text_message");
            to.put("content", message);
            to.put("aid", fromUserInfo.getPhone());
            to.put("meType" ,1);
            to.put("timestamp", new Date().getTime());
            channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(to, SerializerFeature.DisableCircularReferenceDetect)));
            re.put("success", 0);
            re.put("timestamp",new Date().getTime());
        } else {
            re.put("success", 1);
        }
        return JSON.toJSONString(re);
    }

    /**
     * 上传文件
     */
    @PostMapping("/sendImg")
    @ResponseBody
    public String upload(@RequestParam("file") MultipartFile file,@RequestParam(value = "fromSessionId") String fromSessionId, @RequestParam(value = "toSessionId") String toSessionId) throws Exception {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        //上传文件
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String url = OSSFactory.build().uploadSuffix(file.getBytes(), suffix);
        UserInfo fromUserInfo = ChatConstants.onlines.get(fromSessionId);
        UserInfo toUserInfo = ChatConstants.onlines.get(toSessionId);
        Channel channel = ChatConstants.onlineChannel.get(toSessionId);
        Map<String, Object> re = new HashMap<>();
        if (channel != null) {
            String token = channel.attr(ChatConstants.CHANNEL_TOKEN_KEY).get();
            System.out.println(token);
            Map<String, Object> to = new HashMap<>();
            to.put(toSessionId, toUserInfo);
            to.put("type", "img_message");
            to.put("url", url);
            to.put("aid", fromUserInfo.getPhone());
            to.put("meType" ,2);
            to.put("timestamp", new Date().getTime());
            channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(to, SerializerFeature.DisableCircularReferenceDetect)));
            re.put("success", 0);
            re.put("url",url);
            re.put("timestamp",new Date().getTime());
        } else {
            re.put("success", 1);
        }
        return JSON.toJSONString(re);
    }

    /**
     * 用户登录
     *
     * @param user
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    public String doLogin(@RequestBody UserInfo user) {
        UserInfo.map.put(user.getPhone(), user);
        Map<String, Object> re = new HashMap<>();
        re.put("phone", user.getPhone());
        return JSON.toJSONString(re);
    }

}
