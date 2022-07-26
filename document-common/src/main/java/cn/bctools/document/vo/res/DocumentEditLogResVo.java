package cn.bctools.document.vo.res;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: ZhuXiaoKang
 * @Description:  知识库-查询文档编辑记录出参
 */

@Data
@ApiModel("知识库-查询文档编辑记录出参")
public class DocumentEditLogResVo {

    @ApiModelProperty(value = "操作人")
    private String userName;

    @ApiModelProperty(value = "操作时间")
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    private LocalDateTime createTime;
}
