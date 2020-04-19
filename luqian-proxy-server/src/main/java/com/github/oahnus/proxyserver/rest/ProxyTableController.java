package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.proxycommon.RespData;
import com.github.oahnus.proxyserver.config.ProxyTable;
import com.github.oahnus.proxyserver.entity.ProxyTableItem;
import com.github.oahnus.proxyserver.utils.AESUtils;
import com.github.oahnus.proxyserver.utils.RandomUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oahnus on 2020-04-16
 * 7:16.
 */
@RestController
@RequestMapping("/proxyTable")
public class ProxyTableController {
    @GetMapping("/add")
    public RespData addPortMapping(@RequestParam Integer port,
                                   @RequestParam String appId,
                                   @RequestParam String hostPort) {

        ProxyTable instance = ProxyTable.getInstance();
        String secret = instance.getAppSecret(appId);
        if (secret == null) {
            return RespData.error(403, "用户不存在");
        }

        ProxyTableItem proxyMapping = instance.getProxyMapping(port);
        if (proxyMapping != null) {
            return RespData.error(403, "端口已被占用");
        }

        boolean res = instance.addOutPort(appId, port);
        if (res) {
            instance.addPort2ServiceMapping(port, hostPort);
            instance.notifyObservers();
        } else {
            return RespData.error(401, "单个用户最多创建3条代理映射");
        }
        return new RespData();
    }

    @GetMapping("/remove")
    public RespData removePortMapping(@RequestParam String appId,
                                  @RequestParam Integer port) {
        String secret = ProxyTable.getInstance().getAppSecret(appId);
        if (secret == null) {
            return RespData.error(403, "用户不存在");
        }
        ProxyTableItem tableItem = ProxyTable.getInstance().getProxyMapping(port);
        if (tableItem.getAppId().equals(appId)) {
            ProxyTable.getInstance().removeMapping(port);
            ProxyTable.getInstance().notifyObservers();
        }
        return new RespData();
    }

    @GetMapping("gen")
    public RespData genAppIDAndSecret() {
        int maxTry = 5, i = 0;
        String appId = null, secret = null;
        while (i < maxTry) {
            appId = RandomUtil.genNChars(8, RandomUtil.MODE.ONLY_NUMBER);
            if (ProxyTable.getInstance().getAppSecret(appId) == null) {
                secret = AESUtils.encrypt(appId);
                ProxyTable.getInstance().addUser(appId, secret);
                break;
            }
            i++;
        }
        if (secret == null) {
            return RespData.error(400, "密钥生成失败");
        }
        Map<String, String> res = new HashMap<>(4);
        res.put("appId", appId);
        res.put("appSecret", secret);

        return RespData.success(res);
    }
}
