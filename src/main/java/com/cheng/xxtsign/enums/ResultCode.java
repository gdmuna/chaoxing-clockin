package com.cheng.xxtsign.enums;

/**
 * @author xiaoc
 * 系统返回码
 */
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(404, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOTDATA(405, "未查找到对应数据"),
    REFRESHTOKENEXCEPTION(406, "refresh-token异常"),
    ORDER_EXCEPTION(407, "order异常"),
    USERNAMEEXCEPTION(1040001, "账号不存在"),
    USERPASSWORDEXCETION(1040002, "密码不正确"),
    EXIST_PHONE(1040003, "此电话号码已经注册"),
    SYSEXCEPTION(999, "系统异常");
    private long code;
    private String message;

    private ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    public long getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
