package com.cheng.xxtsign.controller;

import com.cheng.xxtsign.common.CommonResult;
import com.cheng.xxtsign.service.XXTSignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sign")
public class XXTSignController {

    @Autowired
    private XXTSignService xxtSignService;

    /**
     * 普通签到 位置签到
     * 帮一个组的人统一签到
     * @param mark
     * @return
     */
    @GetMapping("/group/general")
    public CommonResult generalGroup(@RequestParam("mark") String mark, @RequestParam("lo") String location) {
        return CommonResult.success(xxtSignService.generalGroupSign(mark, location));
    }

    /**
     * 位置签到
     * 帮一个组的人统一签到
     */
//    @GetMapping("/group/location")
//    public CommonResult location(@RequestParam("lo") String longitude, @RequestParam("lat") String latitude) {
//
//        return CommonResult.success("全部签到完成");
//    }

    /**
     * 普通签到 位置签到
     */
    @GetMapping("/general")
    public CommonResult general(@RequestParam("phone") String phone, @RequestParam("lo") String location) {
        return CommonResult.success(xxtSignService.generalSign(phone, location));
    }

    /**
     * 签到码签到
     * @param longitude
     * @param latitude
     * @return
     */
//    @GetMapping("/group/location")
//    public CommonResult location(@RequestParam("lo") String longitude, @RequestParam("lat") String latitude) {
//
//        return CommonResult.success("全部签到完成");
//    }
}
