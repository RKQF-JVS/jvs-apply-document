package cn.bctools.document.vo.res;

import cn.bctools.document.entity.DcLibraryUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库人员列表响应
 */

@Data
@ApiModel("知识库-人员列表")
public class DcLibraryMemberResVo extends DcLibraryUser {

    @ApiModelProperty(value = "用户头像")
    private String headImg;
}
