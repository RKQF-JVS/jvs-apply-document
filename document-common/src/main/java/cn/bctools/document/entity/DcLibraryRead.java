package cn.bctools.document.entity;

import cn.hutool.core.date.DatePattern;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.*;
import java.io.Serializable;

/**
 * @author auto
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("知识库-用户已读记录")
@EqualsAndHashCode(callSuper = false)
@TableName("dc_library_read")
public class DcLibraryRead implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    @DateTimeFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN, timezone = "GMT+8")
    private LocalDateTime createTime;
    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    @ApiModelProperty("知识库id")
    @TableField("knowledge_id")
    private String knowledgeId;
    @ApiModelProperty("用户名-标记为已读时，必须要登录")
    @TableField("name")
    private String name;
    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;
    @ApiModelProperty("租户")
    @TableField("tenant_id")
    private String tenantId;
}
