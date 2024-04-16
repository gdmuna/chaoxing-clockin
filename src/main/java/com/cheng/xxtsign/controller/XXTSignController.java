package com.cheng.xxtsign.controller;

import com.cheng.xxtsign.common.CommonResult;
import com.cheng.xxtsign.service.XXTSignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 各种签到的接口
 */
@RestController
@RequestMapping("/sign")
public class XXTSignController {

    @Autowired
    private XXTSignService xxtSignService;

    /**
     * 普通签到 位置签到 签到码签到 手势签到
     * 帮一个组的人统一签到
     * @param mark 组标识
     * @return 返回一个list,标明签到者和签到关系
     */
    @GetMapping("/group/general")
    public CommonResult generalGroup(@RequestParam("mark") String mark, @RequestParam("lo") String location) {
        return CommonResult.success(xxtSignService.generalGroupSign(mark, location));
    }

    /**
     * 普通签到 位置签到 签到码签到 手势签到
     * 个人签到
     * @param phone 个人的电话号码（xxt账号）
     * @param location 位置参数，参考枚举LocationSignEnum，给定你的位置参数，不是位置签到不会使用此参数（填什么都可以）
     * @return 用户：签到成功|签到失败
     */
    @GetMapping("/general")
    public CommonResult general(@RequestParam("phone") String phone, @RequestParam("lo") String location) {
        return CommonResult.success(xxtSignService.generalSign(phone, location));
    }
}
