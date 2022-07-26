package cn.bctools.document.constant;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * @Author: ZhuXiaoKang
 * @Description: ES索引常量
 */
public class IndexConstant {
    private IndexConstant() {
    }

    /**
     * 知识库基本信息(DocumentEsPo)
     */
    public static final IndexCoordinates INDEX_DOCUMENT_BASE_INFO = IndexCoordinates.of("document_base_info");

    /**
     * 知识库文档操作日志（DocumentLogEsPo）
     */
    public static final IndexCoordinates INDEX_DOCUMENT_LOG = IndexCoordinates.of("document_log");
}
