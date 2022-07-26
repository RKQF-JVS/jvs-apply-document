package cn.bctools.document.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-分享设置类型枚举
 */
@Getter
public enum ShareSettingTypeEnum {

    /**
     * 知识库-分享设置类型枚举
     */
    CREATE_URL("CREATE_URL", "生成分享链接"),
    VALIDITY("VALIDITY", "时效"),
    PWD("PWD", "密码"),
    SHARE("SHARE", "分享开关");

    @EnumValue
    public final String value;
    public final String desc;

    ShareSettingTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ShareSettingTypeEnum getByValue(String value) {
        for(ShareSettingTypeEnum currentEnum : ShareSettingTypeEnum.values()){
            if(value == currentEnum.value){
                return currentEnum;
            }
        }
        return null;
    }
}
