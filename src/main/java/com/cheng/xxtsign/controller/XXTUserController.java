package com.cheng.xxtsign.controller;

import com.cheng.xxtsign.common.CommonResult;
import com.cheng.xxtsign.service.XXTUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class XXTUserController {
    @Autowired
    private XXTUserService xxtUserService;
    /**
     * 用户登录接口
     */
    @GetMapping("/login")
    public CommonResult userLogin(@RequestParam("phone") String phone, @RequestParam("password") String password) {
        xxtUserService.userLogin(phone, password);

        return CommonResult.success("登录成功");
    }

    /**
     * 用户加入组
     * @param mark 标识字符串
     * @return
     */
    @GetMapping("/join")
    public CommonResult joinGroup(@RequestParam("mark") String mark) {
        return CommonResult.success("加入成功");
    }
}
