package cn.bctools.document.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author : GaoZeXi
 */
@Data
@Accessors(chain = true)
@ApiModel("添加用户成员Dto")
public class DcUserDto {
    @ApiModelProperty("用户成员id")
    private List<String> userIds;
    @ApiModelProperty("知识库Id")
    @NotNull(message = "文档Id不允许为空")
    private String documentId;
}
