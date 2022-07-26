package cn.bctools.document.entity;

import cn.hutool.core.date.DatePattern;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author auto
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("知识库-用户留言")
@EqualsAndHashCode(callSuper = false)
@TableName("dc_library_comment")
public class DcLibraryComment implements Serializable {

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
    @ApiModelProperty("留言")
    @TableField("message")
    private String message;
    @ApiModelProperty("用户名")
    @TableField("name")
    private String name;
    @ApiModelProperty("用户id")
    private String userId;
    @ApiModelProperty("上级评论")
    @TableField("parent_id")
    private String parentId;
}
