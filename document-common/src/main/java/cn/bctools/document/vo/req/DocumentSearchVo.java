package cn.bctools.document.vo.req;

import cn.bctools.document.vo.BaseReqVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库文档搜索入参
 */

@Data
@Accessors(chain = true)
@ApiModel("知识库-搜索")
public class DocumentSearchVo extends BaseReqVo {

    @ApiModelProperty(value = "知识库id")
    private String knowledgeId;

    @ApiModelProperty(value = "关键词")
    private String keyword;
}
