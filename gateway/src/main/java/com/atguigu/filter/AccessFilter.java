package com.atguigu.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AccessFilter implements GlobalFilter {

    //filterWhiteList=trade.html,myOrder.html,list.html
    @Value("${filter.whiteList}")
    private String filterWhiteList;

    @Autowired
    private RedisTemplate redisTemplate;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if(antPathMatcher.match("/sku/**",path)){
            ServerHttpResponse response = exchange.getResponse();
            return writeDataToBrowser(response,RetValCodeEnum.NO_PERMISSION);
        }

        String userId = getLoginUserId(request);
        String userTempId = getUserTempId(request);

        if ("-1".equals(userId)){
            ServerHttpResponse response = exchange.getResponse();
            return writeDataToBrowser(response,RetValCodeEnum.NO_PERMISSION);
        }

        if (antPathMatcher.match("/order/**",path)){
            //如果是未登录情况
            if (StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                return writeDataToBrowser(response, RetValCodeEnum.NO_LOGIN);
            }
        }

        for (String filterWhite : filterWhiteList.split(",")) {
            if(path.indexOf(filterWhite) != -1 && StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originalUrl="+request.getURI());
                return response.setComplete();
            }
        }
//        if(StringUtils.isEmpty(userId) || StringUtils.isEmpty(userTempId)){
//            if(!StringUtils.isEmpty(userId)){
//                // 网关需要再次设置userId给后面的服务使用,后面微服务远程调用也是用拦截器自己添加userId到head里面
//                request.mutate().header("userId",userId);
//                return chain.filter(exchange.mutate().request(request).build());
//            }
//            if(!StringUtils.isEmpty(userTempId)){
//                // 网关需要再次设置userTempId给后面的服务使用,后面微服务远程调用也是用拦截器自己添加userTempId到head里面
//                request.mutate().header("userTempId",userTempId);
//                return chain.filter(exchange.mutate().request(request).build());
//            }
//        }
        if(!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){
            if(!StringUtils.isEmpty(userId)){
                //将用户id放入到header中
                request.mutate().header("userId",userId).build();
            }
            if(!StringUtils.isEmpty(userTempId)){
                //将用户id放入到header中
                request.mutate().header("userTempId",userTempId).build();
            }
            //过滤器放开拦截 让下游继续执行(此时exchange里面的header做了修改)
            return chain.filter(exchange.mutate().request(request).build());
        }

        return chain.filter(exchange);
    }

    private String getLoginUserId(ServerHttpRequest request) {
        String token = null;

        List<String> result = request.getHeaders().get("token");
        if(!CollectionUtils.isEmpty(result)){
            token = result.get(0);
         }else{
            HttpCookie cookie = request.getCookies().getFirst("token");
            if(null != cookie){
                token = cookie.getValue();
            }
        }
        if(!StringUtils.isEmpty(token)){
            String userKey = "user:login:"+ token;
            String loginfo = (String) redisTemplate.opsForValue().get(userKey);
            JSONObject loginInfoJson = JSONObject.parseObject(loginfo);
            if (!StringUtils.isEmpty(loginInfoJson)) {
                String loginIp = loginInfoJson.getString("loginIP");
                String gatwayIpAddress = IpUtil.getGatwayIpAddress(request);
                if (!StringUtils.isEmpty(loginIp) && gatwayIpAddress.equals(loginIp)){
                    return loginInfoJson.getString("userId");
                }else {
                    return "-1";
                }
            }
        }
        return null;
    }

//    private String getuserTempId(ServerHttpRequest request) {
//        String userTempId = null;
//
//        List<String> result = request.getHeaders().get("userTempId");
//        if(!CollectionUtils.isEmpty(result)){
//            userTempId = result.get(0);
//         }else{
//            HttpCookie cookie = request.getCookies().getFirst("userTempId");
//            if(null != cookie){
//                userTempId = cookie.getValue();
//            }
//        }
//        return userTempId;
//    }

    private String getUserTempId(ServerHttpRequest request) {
        // 获取header中的数据
        String userTempId = "";
        List<String> result = request.getHeaders().get("userTempId");
        // 判断集合不为空！
        if (!CollectionUtils.isEmpty(result)){
            userTempId = result.get(0);
        }else {
            // 表示在header 中没有获取到数据，从cookie中获取
            HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
            if (httpCookie!=null){
                userTempId = httpCookie.getValue();
            }
        }
        return userTempId;
    }

    private Mono<Void> writeDataToBrowser(ServerHttpResponse response,RetValCodeEnum retValCodeEnum) {
        RetVal retVal = RetVal.build(null, retValCodeEnum);
        byte[] bytes = JSONObject.toJSONString(retVal).getBytes();
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        return response.writeWith(Mono.just(dataBuffer));
    }
}
