package cn.bctools.document.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 知识库阅读权限枚举
 *
 * @author guojing
 */
@Getter
public enum DcLibraryReadEnum {
    /**
     * 用户开放
     */
    user("user", "成员开放"),
    /**
     * 注册用户
     **/
    register("register", "注册用户"),
    /**
     * 完全开放
     **/
    all("all", "完全开放"),
    ;
    @EnumValue
    public final String value;
    public final String desc;

    DcLibraryReadEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

}
