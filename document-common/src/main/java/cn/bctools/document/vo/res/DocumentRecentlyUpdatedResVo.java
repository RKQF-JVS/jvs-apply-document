package cn.bctools.document.vo.res;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.po.enums.DocumentLogTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-最近更新文档出参
 */

@Data
@ApiModel("知识库-最近更新文档出参")
public class DocumentRecentlyUpdatedResVo {

    @ApiModelProperty(value = "文档id")
    private String docId;

    @ApiModelProperty(value = "文档名称")
    private String name;

    @ApiModelProperty(value = "类型")
    private DcLibraryTypeEnum type;

    @ApiModelProperty(value = "操作类型")
    private DocumentLogTypeEnum logType;

    @ApiModelProperty(value = "操作时间")
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "操作人")
    @Field(type = FieldType.Text)
    private String userName;

    @ApiModelProperty(value = "操作人id")
    private String userId;

    @ApiModelProperty(value = "用户头像")
    private String headImg;

}
