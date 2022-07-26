package cn.bctools.document.vo.req;

import cn.bctools.document.entity.enums.DcLibraryLikeTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * @Author: ZhuXiaoKang
 * @Description: 点赞请求
 */
@Data
@Accessors(chain = true)
@ApiModel("知识库-点赞入参")
public class DcLibraryLikeVo{

    @ApiModelProperty(value = "点赞业务类型", required = true)
    @NotNull(message = "点赞业务类型不能为空")
    private DcLibraryLikeTypeEnum bizType;

    @ApiModelProperty(value = "点赞资源id", required = true)
    @NotNull(message = "点赞资源id不能为空")
    private String bizResourceId;
}
