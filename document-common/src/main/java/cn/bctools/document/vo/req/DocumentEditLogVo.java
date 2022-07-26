package cn.bctools.document.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-查询文档编辑记录入参
 */

@Data
@Accessors(chain = true)
@ApiModel("知识库-查询文档编辑记录入参")
public class DocumentEditLogVo {

    @ApiModelProperty(value = "文档id", required = true)
    @NotNull(message = "文档id不能为空")
    private String id;
}
