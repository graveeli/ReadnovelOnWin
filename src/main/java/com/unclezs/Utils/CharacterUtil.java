package com.unclezs.Utils;

import com.luhuiguo.chinese.ChineseUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 *字符转换工具类
 *@author unclezs.com
 *@date 2019.07.05 19:53
 */
public class CharacterUtil {
    /**
     * 繁体转简体
     *
     * @return 简体字符集
     */
    public static String traditional2Simple(String src) {
        return ChineseUtils.toSimplified(src);
    }

    /**
     * 将&#类得字符转化为汉字
     *
     * @param src 字符集&#20491;&#30007;&#20154;&#30475;
     * @return 转码后得字符集
     */
    public static String NCR2Chinese(String src) {
        Pattern pattern = Pattern.compile("&#([\\d]{2,6});");
        src = src.replace("\r\n", "&#92;&#114;&#92;&#110;");//换行符处理
        Matcher m = pattern.matcher(src);
        while (m.find()) {
            src = src.replace(m.group(0), (char) Integer.parseInt(m.group(1)) + "");
        }
        return src.replace("\\r\\n", "\r\n");
    }

}
