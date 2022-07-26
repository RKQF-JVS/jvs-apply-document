package cn.bctools.document.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * @Author: ZhuXiaoKang
 * @Description: 评论请求入参
 */
@Data
@Accessors(chain = true)
@ApiModel("评论请求入参")
public class DcCommentSaveReqVo {

    @ApiModelProperty(value = "知识库id", required = true)
    @NotNull(message = "知识库id不能为空")
    private String knowledgeId;

    @ApiModelProperty(value = "留言", required = true)
    @NotNull(message = "留言不能为空")
    private String message;

    @ApiModelProperty(value = "父id")
    private String parentId;
}
