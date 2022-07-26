package cn.bctools.document.entity;

import cn.hutool.core.date.DatePattern;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.database.entity.po.BasalPo;
import cn.bctools.database.entity.po.BasePo;
import cn.bctools.document.entity.enums.DcLibraryReadEnum;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.entity.enums.DcLibraryShareValidityTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author auto
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("知识库")
@EqualsAndHashCode(callSuper = false)
@TableName("dc_library")
public class DcLibrary extends BasalPo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    @ApiModelProperty("文件存储路径")
    private String filePath;
    @ApiModelProperty("桶名称")
    private String bucketName;
    @ApiModelProperty("名称")
    @NotBlank(message = "标题不能为空")
    @Length(max = 64, min = 1, message = "标题长度需控制在1-64位")
    private String name;
    @ApiModelProperty("阅读权限")
    private DcLibraryReadEnum shareRole;
    @ApiModelProperty(" 分享/不分享，分享")
    private Boolean share;
    @ApiModelProperty("密码模式")
    private String sharePassword;
    @ApiModelProperty("分享结束时间")
    @DateTimeFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN, timezone = "GMT+8")
    private LocalDateTime shareEndTime;
    @ApiModelProperty("分享的文档链接,二维码可通过链接生成即可")
    private String shareLink;
    @ApiModelProperty("分享key")
    private String shareKey;
    @ApiModelProperty("文档大小M")
    private Double size;
    @ApiModelProperty("上级ID")
    private String parentId;
    @ApiModelProperty("类型/知识库、目录、文本文档、表格文档、脑图文档、流程文档。")
    private DcLibraryTypeEnum type;
    @ApiModelProperty("排序")
    private Integer orderId;
    @ApiModelProperty("分享时效类型")
    private DcLibraryShareValidityTypeEnum shareValidityType;
    @ApiModelProperty("是否删除 0未删除  1已删除")
    @TableLogic
    private Boolean delFlag;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("子集")
    @TableField(exist = false)
    private List<DcLibrary> children;

    @ApiModelProperty("正在编辑该文档的用户Id")
    private String editingBy;

    @TableField(exist = false)
    @ApiModelProperty("所有者")
    private Boolean isOwner;

    @ApiModelProperty("内容")
    @TableField(exist = false)
    private String content;
    @ApiModelProperty("知识库id")
    private String knowledgeId;

    @ApiModelProperty("知识库封面颜色")
    private String color;

    @ApiModelProperty(value = "租户")
    private String tenantId;

    @ApiModelProperty(value = "自动发送查看提醒开关")
    private Boolean readNotify;

}
