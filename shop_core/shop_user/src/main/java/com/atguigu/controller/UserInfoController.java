package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-11-08
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("login")
    public RetVal login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        UserInfo dbUser = userInfoService.queryUserFromDb(userInfo);
        if(null != dbUser){
            Map<String, Object> retMap = new HashMap<>();

            String token = UUID.randomUUID().toString();
            String ipAddress = IpUtil.getIpAddress(request);

            retMap.put("token",token);
            retMap.put("nickName",dbUser.getNickName());

            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userInfoJson = new JSONObject();
            userInfoJson.put("userId",dbUser.getId());
            userInfoJson.put("loginIP",ipAddress);
            redisTemplate.opsForValue().set(userKey,userInfoJson.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return RetVal.ok(retMap);
        }else {
            return RetVal.fail().message("登录失败！");
        }
    }

    @GetMapping("/logout")
    public RetVal logout(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for (Cookie cookie : cookies) {
                System.out.println(cookie);
            }
        }
        String token = request.getHeader("token");
        if(!StringUtils.isEmpty(token)){
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            Boolean delete = redisTemplate.delete(userKey);
            return RetVal.ok();
        }else {
            return RetVal.fail();
        }
    }
}

