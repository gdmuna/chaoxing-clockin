package com.cheng.xxtsign.dao.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class XXTUserVO {

    private String name;

    private String phone;

    private String loginTime;

    private String againLoginTime;

    public void setAgainLoginTime(String againLoginTime){
        // 将日期字符串转换为LocalDate对象
        LocalDate parsedDate = LocalDate.parse(againLoginTime);
        LocalDate localDate = parsedDate.plusDays(29);
        this.againLoginTime = localDate.toString();
    }
}
