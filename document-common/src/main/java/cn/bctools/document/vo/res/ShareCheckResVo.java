package cn.bctools.document.vo.res;

import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: ZhuXiaoKang
 * @Description: 校验分享链接响应
 */

@Data
@ApiModel("校验分享链接响应")
public class ShareCheckResVo {

    @ApiModelProperty(value = "是否校验密码。true-是,false-否")
    private Boolean needPwd;

    @ApiModelProperty(value = "校验结果。true-成功,false-失败")
    private Boolean check;

    @ApiModelProperty(value = "当前分享的资源id（知识库id、文档id）")
    private String id;

    @ApiModelProperty(value = "类型/知识库、目录、文本文档、表格文档、脑图文档、流程文档。")
    private DcLibraryTypeEnum type;

    @ApiModelProperty(value = "分享状态。true-正常，false-已停止分享,或分享不存在")
    private Boolean shareStatus;

    @ApiModelProperty(value = "知识库id")
    private String knowledgeId;

    @ApiModelProperty(value = "租户id")
    private String tenantId;
}
