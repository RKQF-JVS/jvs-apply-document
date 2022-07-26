package cn.bctools.document.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 知识库阅读权限枚举
 * @author guojing
 */
@Getter
public enum DcLibraryUserRoleEnum {
    /**
     * 所有者、、
     */
    owner("owner","owner"),
    /**
     * 管理员
     **/
    admin("admin","admin"),
    /**
     * 成员
     **/
    member("member","member"),
    ;
    @EnumValue
    public final String value;
    public final String desc;

    DcLibraryUserRoleEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
