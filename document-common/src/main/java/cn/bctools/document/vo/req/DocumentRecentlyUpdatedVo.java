package cn.bctools.document.vo.req;

import cn.bctools.document.vo.BaseReqVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-最近更新文档 入参
 */

@Data
@Accessors(chain = true)
@ApiModel("知识库-最近更新文档入参")
public class DocumentRecentlyUpdatedVo extends BaseReqVo {

    @ApiModelProperty(value = "知识库id或目录id", required = true)
    @NotNull(message = "知识库id或目录id不能为空")
    private String id;

    @ApiModelProperty(value = "返回数量", required = true)
    @NotNull(message = "返回数量不能为空")
    private Integer size;
}
