package cn.bctools.document.po;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.po.enums.DocumentLogTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库文档操作日志
 */

@Data
@ApiModel("知识库文档操作日志")
@Document(indexName = "document_log")
public class DocumentLogEsPo {

    @ApiModelProperty(value = "索引id", notes = "数据格式：租户id_文档id_时间戳")
    @Id
    private String id;

    @ApiModelProperty(value = "文档id")
    @Field(type = FieldType.Keyword)
    private String docId;

    @ApiModelProperty(value = "类型")
    @Field(type = FieldType.Keyword)
    private DcLibraryTypeEnum type;

    @ApiModelProperty(value = "日志类型")
    @Field(type = FieldType.Keyword)
    private DocumentLogTypeEnum logType;

    @ApiModelProperty(value = "租户id")
    @Field(type = FieldType.Keyword)
    private String tenantId;

    @ApiModelProperty(value = "文档名称")
    @Field(type = FieldType.Text)
    private String name;

    @ApiModelProperty(value = "操作时间")
    @Field(type =  FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePattern.NORM_DATETIME_PATTERN, timezone = "GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "操作人Id")
    @Field(type = FieldType.Keyword)
    private String userId;

    @ApiModelProperty(value = "操作人")
    @Field(type = FieldType.Text)
    private String userName;

    @ApiModelProperty(value = "知识库名称")
    @Field(type = FieldType.Text)
    private String knowledgeName;

    @ApiModelProperty(value = "知识库id")
    @Field(type = FieldType.Keyword)
    private String knowledgeId;

}
