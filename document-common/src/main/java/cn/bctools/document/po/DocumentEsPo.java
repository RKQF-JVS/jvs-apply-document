package cn.bctools.document.po;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
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
 * @Description: 知识库基本信息(包含所有知识库类型) ES实体类
 */
@Data
@ApiModel("知识库基本信息")
@Document(indexName = "document_base_info")
public class DocumentEsPo {

    @ApiModelProperty(value = "索引id", notes = "数据格式：租户id_文档id")
    @Id
    private String id;

    @ApiModelProperty(value = "文档id")
    @Field(type = FieldType.Keyword)
    private String docId;

    @ApiModelProperty(value = "类型")
    @Field(type = FieldType.Keyword)
    private DcLibraryTypeEnum type;

    @ApiModelProperty(value = "租户id")
    @Field(type = FieldType.Keyword)
    private String tenantId;

    @ApiModelProperty(value = "文档名称")
    @Field(type = FieldType.Text)
    private String name;

    @ApiModelProperty(value = "文档内容")
    @Field(type = FieldType.Text)
    private String content;

    @ApiModelProperty(value = "知识库名称")
    @Field(type = FieldType.Text)
    private String knowledgeName;

    @ApiModelProperty(value = "知识库id")
    @Field(type = FieldType.Keyword)
    private String knowledgeId;

    @ApiModelProperty(value = "文档创建时间")
    @Field(type =  FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePattern.NORM_DATETIME_PATTERN, timezone = "GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "上级id")
    @Field(type = FieldType.Keyword)
    private String parentId;

    @ApiModelProperty(value = "作者id")
    @Field(type = FieldType.Text)
    private String authorId;

    @ApiModelProperty(value = "作者名称")
    @Field(type = FieldType.Text)
    private String authorName;
}
