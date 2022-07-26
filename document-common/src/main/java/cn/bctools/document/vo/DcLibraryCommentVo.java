package cn.bctools.document.vo;

import cn.bctools.document.entity.DcLibraryComment;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @Author wenxin
 * @Version 1.0
 **/
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("知识库-用户留言")
public class DcLibraryCommentVo extends DcLibraryComment {

    @ApiModelProperty("下级留言")
    private boolean children;

    @ApiModelProperty("用户头像")
    private String headImg;

    @ApiModelProperty("点赞次数")
    private Integer likeTotal;

    @ApiModelProperty("当前用户是否已点赞：true-已点赞，false-未点赞")
    private boolean currentUserLike;

}
