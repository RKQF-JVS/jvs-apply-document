package cn.bctools.document.po.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库文档日志类型
 */
public enum DocumentLogTypeEnum {

    /**
     * 知识库文档日志类型
     */
    CREATE("CREATE", "新建"),
    EDIT("EDIT", "编辑"),
    READ("READ", "阅读");

    @EnumValue
    private final String value;
    private final String desc;

    DocumentLogTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public static DocumentLogTypeEnum getByValue(String value) {
        for(DocumentLogTypeEnum currentEnum : DocumentLogTypeEnum.values()){
            if(value.equals(currentEnum.value)){
                return currentEnum;
            }
        }
        return null;
    }
}
