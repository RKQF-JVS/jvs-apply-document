package cn.bctools.document.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Author: ZhuXiaoKang
 * @Description: 设置自动发送查看提醒开关。
 */
@Data
@Accessors(chain = true)
@ApiModel("设置自动发送查看提醒开关入参")
public class SettingReadNotifyReqVo {

    @ApiModelProperty(value = "知识库文档id", required = true)
    @NotBlank(message = "知识库文档id不能为空")
    private String id;

    @ApiModelProperty(value = "自动发送查看提醒开关。true-开，false-关", required = true)
    @NotNull(message = "查看提醒开关不能为空")
    private Boolean readNotify;


}
