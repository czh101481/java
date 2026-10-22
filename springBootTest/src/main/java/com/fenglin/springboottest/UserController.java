package com.fenglin.springboottest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user")
public class UserController {
    private Log log = LogFactory.getLog(UserController.class);
    //比如一个应用有三端，网页端、移动端、应用端，在登录的时候，我得判断，判断你是从哪个端进来，
    // 从应用进来就显示应用端的页面，移动端就显示移动端的页面，应用端就显示应用端的页面
    //如何做到这个功能
    @Autowired
    private Environment  environment;
    @RequestMapping("/to_login")
    public String toLogin(){
        return "user/login";
    }
    @RequestMapping("login")
    public String login(@RequestParam("username")String username, String password, Model model){
        if(!password.equals("123")){
            model.addAttribute("msg","密码错误");
            return "forward:to_login";
        }
        model.addAttribute("username",username);
        model.addAttribute("password",password);
        return "user/main";
    }
    @RequestMapping("/to_index")
    public String toIndex(){
        return "redirect:/index.jsp";
    }

    @RequestMapping("/test")
    @ResponseBody
    public String test(){
        String ss = environment.getProperty("spring.application.name")+"   ";
        ss += environment.getProperty("server.port")+"   ";
        ss += environment.getProperty("test")+"   ";
        ss += environment.getProperty("book.bookid")+"   ";
        log.info("日志信息");
        log.debug("日志信息");
        log.error("日志信息");
        log.warn("日志信息");
        log.fatal("日志信息");
        log.trace("日志信息");
        return ss;

    }
}
