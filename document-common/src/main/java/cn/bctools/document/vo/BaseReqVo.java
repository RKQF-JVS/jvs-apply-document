package cn.bctools.document.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 基础请求入参（支持未登录查询知识库信息，或，查询其它租户的知识库。在AOP中设置当前请求使用的租户id）
 */

@Data
@Accessors(chain = true)
public class BaseReqVo {

    @ApiModelProperty(value = "登录用户租户是否与知识库租户相同。true-相同,false-不同")
    private Boolean sameTenant;

    @ApiModelProperty(value = "知识库id(sameTenant为false时必传，用以后端确认租户)")
    private String dcId;
}
