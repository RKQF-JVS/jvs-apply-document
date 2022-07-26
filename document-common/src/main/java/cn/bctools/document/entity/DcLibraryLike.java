package cn.bctools.document.entity;

import cn.hutool.core.date.DatePattern;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.document.entity.enums.DcLibraryLikeTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: ZhuXiaoKang
 * @Description: 点赞
 */

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("知识库-点赞")
@EqualsAndHashCode(callSuper = false)
@TableName("dc_library_like")
public class DcLibraryLike implements Serializable {

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @ApiModelProperty("用户名")
    private String realName;

    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("点赞业务类型")
    private DcLibraryLikeTypeEnum bizType;

    @ApiModelProperty("点赞资源id")
    private String bizResourceId;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    private LocalDateTime createTime;

    @ApiModelProperty("修改")
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    private LocalDateTime updateTime;

}
