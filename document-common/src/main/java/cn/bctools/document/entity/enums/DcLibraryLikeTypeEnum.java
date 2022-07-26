package cn.bctools.document.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-点赞业务类型
 */
@Getter
public enum DcLibraryLikeTypeEnum {

    /**
     * 知识库-点赞业务类型
     */
    LIBRARY("LIBRARY", "知识库"),
    COMMENT("COMMENT", "评论");

    @EnumValue
    public final String value;
    public final String desc;

    DcLibraryLikeTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
