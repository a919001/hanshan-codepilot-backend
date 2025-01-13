package com.hanshan.codepilot.satoken;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.hanshan.codepilot.common.ErrorCode;
import com.hanshan.codepilot.exception.ThrowUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 设备工具类
 */
public class DeviceUtils {

    /**
     * 根据请求获取设备信息
     *
     * @return 设备类型
     */
    public static String getRequestDevice(HttpServletRequest request) {
        String userAgentStr = request.getHeader(Header.USER_AGENT.toString());
        // 使用 Hutool 解析 UserAgent
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        ThrowUtils.throwIf(userAgent == null, ErrorCode.OPERATION_ERROR, "非法请求");
        // 默认值为 PC
        String device = "pc";
        if (isMiniProgram(userAgentStr)) {
            // 小程序
            device = "miniProgram";
        } else if (isPad(userAgentStr)) {
            // Pad
            device = "pad";
        } else if (userAgent.isMobile()) {
            // 手机
            device = "mobile";
        }
        return device;
    }

    /**
     * 判断是否是小程序
     * 一般通过 User-Agent 字符串中的 "MicroMessenger" 来判断是否是微信小程序
     */
    private static boolean isMiniProgram(String userAgentStr) {
        return StrUtil.containsIgnoreCase(userAgentStr, "MicroMessenger")
                && StrUtil.containsIgnoreCase(userAgentStr, "MiniProgram");
    }

    /**
     * 判断是否为平板设备
     * 支持 iOS（如 iPad）和 Android 平板的检测
     */
    private static boolean isPad(String userAgentStr) {
        // 检查 iPad 的 User-Agent 标志
        boolean isIPad = StrUtil.containsIgnoreCase(userAgentStr, "iPad");
        // 检查 Android 平板（包含 "Android" 且不包含 "Mobile"
        boolean isAndroidTablet = StrUtil.containsIgnoreCase(userAgentStr, "Android")
                && !StrUtil.containsIgnoreCase(userAgentStr, "Mobile");
        // 如果是 iPad 或 Android 平板，返回 true
        return isIPad || isAndroidTablet;
    }
}
