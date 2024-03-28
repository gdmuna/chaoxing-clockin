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
        if (xxtUserService.userLogin(phone, password)) {
            return CommonResult.success("登录成功");
        }

        return CommonResult.success("登录失败");
    }

    /**
     * 用户加入组
     * @param mark 标识字符串
     * @return
     */
    @GetMapping("/join")
    public CommonResult joinGroup(@RequestParam("mark") String mark, @RequestParam("phone") String phone) {
        boolean join = xxtUserService.join(mark, phone);
        if (join) {
            return CommonResult.success("加入成功");
        }
        return CommonResult.success("请检查组标识或者手机号码正确性，请确保此号码已经在本系统登录");
    }
}
