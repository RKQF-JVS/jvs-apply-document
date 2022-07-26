package cn.bctools.document.vo.res;

import cn.bctools.document.entity.DcLibrary;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: ZhuXiaoKang
 * @Description: 查询指定目录下(不含子目录)所有文件返回
 */

@Data
@ApiModel("知识库-查询指定目录下(不含子目录)所有文件返回")
public class DocumentResVo extends DcLibrary {

    @ApiModelProperty(value = "作者")
    private String author;
}
