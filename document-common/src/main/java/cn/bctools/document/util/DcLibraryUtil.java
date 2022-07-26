package cn.bctools.document.util;

import cn.bctools.document.constant.Constant;


/**
 * @author : GaoZeXi
 */
public class DcLibraryUtil {

    private DcLibraryUtil() {
    }

    /**
     * 获取随机key 用于存储redis 使用 文档id 作为层级,方便以后 对该key操作,列入 取消分享
     * @return
     */
    public static String getKnowledgeLinkKey(String key){
        return String.format(Constant.KNOWLEDGE_LINK_KEY,key);
    }
}
