package cn.bctools.document.util;

/**
 * @Author: ZhuXiaoKang
 * @Description: html工具
 */
public class HtmlUtil {

    private HtmlUtil() {
    }

    /**
     * 替换html标签
     * @param content
     * @param replace
     * @return
     */
    public static String replaceHtmlTag(String content, String replace) {
        return content.replaceAll("(<[^<]*?>)|(<[\\s]*?/[^<]*?>)|(<[^<]*?/[\\s]*?>)", replace);
    }
}
