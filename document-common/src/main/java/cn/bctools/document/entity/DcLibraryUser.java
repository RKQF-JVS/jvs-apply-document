package cn.bctools.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import cn.bctools.database.entity.po.BasalPo;
import cn.bctools.database.entity.po.BasePo;
import cn.bctools.document.entity.enums.DcLibraryUserRoleEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author auto
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("知识库-文档对应的用户")
@EqualsAndHashCode(callSuper = false)
@TableName("dc_library_user")
public class DcLibraryUser extends BasalPo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    @ApiModelProperty("用户ID")
    private String userId;
    @ApiModelProperty("用户名")
    private String realName;
    @ApiModelProperty("知识库的ID值")
    private String dcLibraryId;
    @ApiModelProperty(" 权限/所有者、管理员、成员")
    private DcLibraryUserRoleEnum role;
    @ApiModelProperty("是否删除 0未删除  1已删除")
    @TableLogic
    private Boolean delFlag;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("租户")
    @TableField("tenant_id")
    private String tenantId;

}
