package com.cheng.xxtsign.controller;

import com.cheng.xxtsign.common.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sign")
public class XXTSignController {

    /**
     * 普通签到
     * 帮一个组的人统一签到
     * @param mark
     * @return
     */
    @GetMapping("/group/general")
    public CommonResult general(@RequestParam("mark") String mark) {

        return CommonResult.success("全部签到完成");
    }

    /**
     * 位置签到
     * 帮一个组的人统一签到
     */
    @GetMapping("/group/location")
    public CommonResult location(@RequestParam("lo") String longitude, @RequestParam("lat") String latitude) {

        return CommonResult.success("全部签到完成");
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
