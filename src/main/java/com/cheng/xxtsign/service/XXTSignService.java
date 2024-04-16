package com.cheng.xxtsign.service;

import java.util.List;

public interface XXTSignService {

    List<String> generalGroupSign(String mark, String location);

    String generalSign(String phone, String location);
}
