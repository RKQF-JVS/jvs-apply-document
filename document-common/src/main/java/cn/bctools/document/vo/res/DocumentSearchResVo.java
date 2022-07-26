package cn.bctools.document.vo.res;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库文档搜索出参
 */

@Data
@ApiModel("知识库文档基本信息")
public class DocumentSearchResVo {

    @ApiModelProperty(value = "文档id")
    private String docId;

    @ApiModelProperty(value = "类型")
    private DcLibraryTypeEnum type;

    @ApiModelProperty(value = "文档名称")
    private String name;

    @ApiModelProperty(value = "文档内容")
    private String content;

    @ApiModelProperty(value = "知识库名称")
    private String knowledgeName;

    @ApiModelProperty(value = "知识库id")
    private String knowledgeId;

    @ApiModelProperty(value = "文档创建时间")
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "作者")
    private String authorName;

    @ApiModelProperty(value = "租户id")
    private String tenantId;

}
