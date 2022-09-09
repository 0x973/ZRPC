package cn.pingbase.zrpc.server;

import cn.pingbase.zrpc.consts.ZRPConstants;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Zak
 * @date 2022/08/18 11:46
 * @description: TODO
 */
@Slf4j
@RestController
public class RemoteServiceController {

    private static final String MEDIA_TYPE_JSON = "application/json;charset=utf-8";

    @PostMapping(value = "/zrpc/{identifier}/{methodName}", produces = MEDIA_TYPE_JSON, consumes = MEDIA_TYPE_JSON)
    public ZRPCResponse zrpcMain(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                 @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer,
                                 @PathVariable("identifier") String identifier,
                                 @PathVariable("methodName") String methodName,
                                 @RequestBody ZRPCRequest request) {
        if (!StringUtils.hasLength(userAgent) || !ZRPConstants.REMOTE_CALLER_USERAGENT.equals(userAgent)) {
            return ZRPCResponse.makeFailResult("Your request was denied.");
        }

        if (!StringUtils.hasLength(identifier)) {
            return ZRPCResponse.makeFailResult("Service identifier can not be empty!");
        }

        if (!StringUtils.hasLength(methodName)) {
            return ZRPCResponse.makeFailResult("Service method name can not be empty!");
        }

        Object beanObject = RemoteServiceBeanStore.get(identifier);
        if (beanObject == null) {
            String reason = String.format("Remote: [target bean could not found, service identifier: %s].", identifier);
            return ZRPCResponse.makeFailResult(reason);
        }

        try {
            return new RemoteServiceHandle(beanObject, methodName, request.getArgs()).invoke();
        } catch (NoSuchMethodException e) {
            return ZRPCResponse.makeFailResult("Service method not found, please check your interface class.");
        } catch (Exception e) {
            log.warn("ZRPC Remote call exception.", e);
            return ZRPCResponse.makeFailResult("Server parsing error, message: " + e.getMessage());
        }
    }
}
