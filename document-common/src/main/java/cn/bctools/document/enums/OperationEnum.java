package cn.bctools.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-操作功能。
 */
@Getter
public enum OperationEnum {

    /**
     * 分享
     */
    SHARE_SETTING("SHARE_SETTING", "分享设置"),
    /**
     * 知识库增删改查
     */
    DC_LIBRARY_DEL("DC_LIBRARY_DEL", "删除知识库(知识库、目录、文档)"),
    DC_LIBRARY_ADD("DC_LIBRARY_ADD", "新增知识库（目录、文档）"),
    DC_LIBRARY_DOCUMENT_SAVE("DC_LIBRARY_DOCUMENT_SAVE", "保存文档"),
    DC_LIBRARY_RENAME("DC_LIBRARY_RENAME", "重命名"),
    DC_LIBRARY_SETTING("DC_LIBRARY_SETTING", "知识库设置"),
    /**
     * 知识库成员设置
     */
    MEMBER("MEMBER", "知识库成员(新增、修改、删除)"),

    /**
     * 通知
     */
    READ_NOTIFY_SETTING("READ_NOTIFY_SETTING", "查看提醒")
    ;

    @EnumValue
    public final String value;
    public final String desc;

    OperationEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static OperationEnum getByValue(String value) {
        for(OperationEnum currentEnum : OperationEnum.values()){
            if(currentEnum.value.equals(value)){
                return currentEnum;
            }
        }
        return null;
    }

}
