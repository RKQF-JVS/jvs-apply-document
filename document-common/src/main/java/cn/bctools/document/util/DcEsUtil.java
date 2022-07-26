package cn.bctools.document.util;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库elastic工具类
 */
public class DcEsUtil {

    private DcEsUtil() {
    }

    /**
     * 知识库索引id
     *
     * @param tenantId 租户id
     * @param id DcLibrary的id
     * @return
     */
    public static String buildEsId(String tenantId, String id) {
        return tenantId + "_" + id;
    }
}
