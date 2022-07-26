package cn.bctools.document.vo.res;

import cn.bctools.document.entity.DcLibraryLike;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: ZhuXiaoKang
 * @Description: 点赞响应
 */

@Data
@ApiModel("知识库-点赞响应")
public class DcLibraryLikeResVo extends DcLibraryLike {

    @ApiModelProperty("用户头像")
    private String headImg;
}
