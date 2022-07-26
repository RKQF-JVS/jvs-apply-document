package cn.bctools.document.dto;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.bctools.document.entity.enums.DcLibraryReadEnum;
import cn.bctools.document.entity.enums.DcLibraryShareValidityTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @author : GaoZeXi
 */
@Data
@Accessors(chain = true)
@ApiModel("知识库文件链接参数相关Dto")
public class DocumentLinkDto {
    /**
     * id
     */
    @NotNull(message = "id不允许为空")
    @ApiModelProperty("文件或知识库 或目录id")
    String id;

    /**
     * 链接校验类型
     */
    @ApiModelProperty("链接校验类型")
    DcLibraryShareValidityTypeEnum validityType;

    @ApiModelProperty("阅读权限")
    private DcLibraryReadEnum shareRole;

    @ApiModelProperty("分享开始时间 ,perpetual-[当前时间-2099/12/31]\" +\n" +
            "                                 \"time_horizon-[当前时间-截止时间]\" +\n" +
            "                                 \"set_time-[指定起始时间-指定结束时间]\" +\n" +
            "                                 \"password-可不传")
    @DateTimeFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN, timezone = "GMT+8")
    private LocalDateTime shareStartTime = LocalDateTime.now();

    @ApiModelProperty("分享结束时间 ,perpetual-[当前时间-2099/12/31]\" +\n" +
            "                                 \"time_horizon-[当前时间-截止时间]\" +\n" +
            "                                 \"set_time-[指定起始时间-指定结束时间]\" +\n" +
            "                                 \"password-可不传")
    @DateTimeFormat(pattern = DatePattern.NORM_DATETIME_PATTERN)
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_PATTERN, timezone = "GMT+8")
    private LocalDateTime shareEndTime = LocalDateTime.now().plusYears(70);

    @ApiModelProperty("密码")
    private String sharePassword;
}
