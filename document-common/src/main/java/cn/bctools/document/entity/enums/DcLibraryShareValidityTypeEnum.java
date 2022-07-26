package cn.bctools.document.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 知识库有效时间类型枚举
 * @author guojing
 */
@Getter
public enum DcLibraryShareValidityTypeEnum {
    /**
     * 永久有效
     */
    PERPETUAL("PERPETUAL","永久有效"),
    /**
     * 1天有效
     **/
    ONE_DAY("ONE_DAY","1天有效"),
    /**
     * 7天有效
     **/
    SEVEN_DAY("SEVEN_DAY","7天有效"),
    /**
     * 30天有效
     */
    THIRTY_DAY("THIRTY_DAY","30天有效")
    ;
    @EnumValue
    public final String value;
    public final String desc;

    DcLibraryShareValidityTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
