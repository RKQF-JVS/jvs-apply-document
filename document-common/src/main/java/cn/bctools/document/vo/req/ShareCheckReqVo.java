package cn.bctools.document.vo.req;

import cn.bctools.document.vo.BaseReqVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 校验分享链接入参
 */

@Data
@Accessors(chain = true)
@ApiModel("校验分享链接入参")
public class ShareCheckReqVo extends BaseReqVo {

    @ApiModelProperty(value = "key")
    private String key;

    @ApiModelProperty(value = "分享密码,加密传输")
    private String pwd;
}
