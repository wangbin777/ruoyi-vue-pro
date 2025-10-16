package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议转换
 */
@RestController
@Slf4j
@RequestMapping("/protocol")
@RequiredArgsConstructor
public class ProtocolController {

    /**
     * 转换协议数据
     * @param json
     * @return
     */
    @PostMapping("/convertData")
    public CommonResult<?> convertData(@RequestBody JSONObject json) {
        return CommonResult.success(ProtocolConverterUtils.objToHex(json.getString("messageId"),json.getString("method"),json));
    }


}
