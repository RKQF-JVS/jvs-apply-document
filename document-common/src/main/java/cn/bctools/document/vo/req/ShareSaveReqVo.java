package cn.bctools.document.vo.req;

import cn.bctools.document.entity.enums.DcLibraryShareValidityTypeEnum;
import cn.bctools.document.entity.enums.ShareSettingTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Author: ZhuXiaoKang
 * @Description: 保存分享设置入参
 */

@Data
@Accessors(chain = true)
@ApiModel("保存分享设置入参")
public class ShareSaveReqVo {

    @ApiModelProperty(value = "id(可以是知识库id、目录id、文件id)", required = true)
    @NotBlank(message = "id不能为空")
    private String id;

    @ApiModelProperty(value = "分享设置类型", notes = "根据类型，指定变更配置", required = true)
    @NotNull(message = "分享设置类型不能为空")
    private ShareSettingTypeEnum settingType;

    @ApiModelProperty(value = "分享时效")
    private DcLibraryShareValidityTypeEnum validityType;

    @ApiModelProperty(value = "分享密码,加密传输")
    private String pwd;

    @ApiModelProperty(value = "true-分享,false-停止分享")
    private Boolean share;
}
