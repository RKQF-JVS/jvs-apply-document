package cn.bctools.document.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 知识库类型枚举
 *
 * @author guojing
 */
@Getter
public enum DcLibraryTypeEnum {
    /**
     * 知识库
     */
    knowledge("knowledge", "知识库"),
    /**
     * 目录
     **/
    directory("directory", "目录"),
    /**
     * 富文本文档
     **/
    document_html("document_html", "富文本文档"),
    /**
     * 表格文档
     */
    document_xlsx("document_xlsx", "表格文档"),
    /**
     * 脑图文档
     */
    document_map("document_map", "脑图文档"),
    /**
     * 流程文档
     */
    document_flow("document_flow", "流程文档"),
    /**
     * 无法识别的文档
     */
    document_unrecognized("document_unrecognized", "无法识别的文档"),
    ;
    @EnumValue
    public final String value;
    public final String desc;

    DcLibraryTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

}
