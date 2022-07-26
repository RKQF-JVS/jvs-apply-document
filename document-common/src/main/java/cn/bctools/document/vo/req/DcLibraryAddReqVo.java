package cn.bctools.document.vo.req;

import cn.bctools.document.entity.enums.DcLibraryReadEnum;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

/**
 * @Author: ZhuXiaoKang
 * @Description: 添加知识库、目录、文档
 */
@Data
@Accessors(chain = true)
@ApiModel("添加知识库、目录、文档入参")
public class DcLibraryAddReqVo {

    @ApiModelProperty(value = "名称")
    @Length(min = 1, max = 125, message = "名称不能为空且不能过长")
    private String name;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "知识库封面颜色")
    private String color;

    @ApiModelProperty(value = "除创建知识库不传递，其余都要传递当前节点的id")
    private String id;

    @ApiModelProperty(value = "创建的类型")
    private DcLibraryTypeEnum fileType;

    @ApiModelProperty(value = "查看权限")
    private DcLibraryReadEnum shareRole;

    @ApiModelProperty(value = "查看提醒开关（true-开启,false-关闭）")
    private Boolean readNotify;
}
