package com.cheng.xxtsign.vo;

import lombok.Data;

@Data
public class UserLoginVo {

    private Integer fid;

    private String uname;

    private String password;

    private String refer;

    private String t;

    private Integer forbidotherlogin;

    private String validate;

    private Integer doubleFactorLogin;

    private Integer independentId;

    private Integer independentNameId;

    public UserLoginVo() {
        fid = -1;
        t = "true";
        forbidotherlogin = 0;
        doubleFactorLogin = 0;
        independentId = 0;
        independentNameId = 0;
    }
}
