package cn.bctools.document.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: ZhuXiaoKang
 * @Description: 下载
 */

@Data
@ApiModel("下载")
public class DcLibraryDownloadResVo {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "byte")
    private byte[] bytes;
}
