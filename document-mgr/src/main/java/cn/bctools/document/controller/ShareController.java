package cn.bctools.document.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import cn.bctools.common.exception.BusinessException;
import cn.bctools.common.utils.R;
import cn.bctools.document.component.RoleComponent;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.enums.OperationEnum;
import cn.bctools.document.service.DcLibraryService;
import cn.bctools.document.vo.req.ShareCheckReqVo;
import cn.bctools.document.vo.req.ShareSaveReqVo;
import cn.bctools.document.vo.res.ShareCheckResVo;
import cn.bctools.log.annotation.Log;
import cn.bctools.oauth2.utils.UserCurrentUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库分享
 */

@Slf4j
@Api(tags = "分享")
@RestController
@RequestMapping(value = "/dcLibrary/share")
@AllArgsConstructor
public class ShareController {

    private final RoleComponent roleComponent;
    private final DcLibraryService dcLibraryService;

    @Log
    @ApiOperation(value = "分享设置")
    @PostMapping("/setting")
    public R<DcLibrary> shareSetting(@RequestBody @Validated ShareSaveReqVo shareSaveReqVo) {
        // 操作权限校验
        roleComponent.checkOperationAuthority(OperationEnum.SHARE_SETTING, UserCurrentUtils.getUserId(), shareSaveReqVo.getId());
        return R.ok(dcLibraryService.settingShare(shareSaveReqVo));
    }

    @Log
    @ApiOperation(value = "校验分享链接")
    @PostMapping("/check")
    public R<ShareCheckResVo> share(@RequestBody @Validated ShareCheckReqVo reqVo) {
        if (StringUtils.isBlank(reqVo.getKey())) {
            throw new BusinessException("分享链接key不能为空");
        }
        return R.ok(dcLibraryService.checkShare(reqVo));
    }
}
