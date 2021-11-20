package com.atguigu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {


    // http://passport.gmall.com/login.html?originalUrl=http://item.gmall.com/
    @GetMapping("/login.html")
    public String login(HttpServletRequest request){
        String originalUrl = request.getParameter("originalUrl");
        String skuNum = request.getParameter("skuNum");
        if(!StringUtils.isEmpty(skuNum)){
        System.out.println(originalUrl + "  " + skuNum);
        originalUrl = originalUrl + "&skuNum=" + skuNum;
        }
        request.setAttribute("originalUrl",originalUrl);
        return "login";
    }

}
