package cn.bctools.document.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.bctools.common.utils.R;
import cn.bctools.document.entity.DcLibraryRead;
import cn.bctools.document.service.DcLibraryReadService;
import cn.bctools.log.annotation.Log;
import cn.bctools.oauth2.utils.UserCurrentUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @Author wenxin
 * @Desc   知识库-用户已读记录
 *
**/
@Api(tags = "用户已读记录【废弃】")
@RestController
@AllArgsConstructor
@RequestMapping("/dcLibraryRead")
public class DcLibraryReadController {

    private final DcLibraryReadService service;

    @Log
    @ApiOperation("分页")
    @GetMapping("/page/{knowledgeId}")
    public R<Page<DcLibraryRead>> page(Page page, @ApiParam(value = "知识库id", required = true) @PathVariable("knowledgeId") String knowledgeId) {
        Page<DcLibraryRead> result = service.page(page, Wrappers.<DcLibraryRead>lambdaQuery().eq(DcLibraryRead::getKnowledgeId, knowledgeId).orderByDesc(DcLibraryRead::getCreateTime));
        return R.ok(result);
    }

    @Log
    @ApiOperation("新增")
    @PostMapping("/save/{knowledgeId}")
    public R<DcLibraryRead> save(@ApiParam(value = "知识库id", required = true) @PathVariable("knowledgeId") String knowledgeId,
                                 @ApiParam("备注") @RequestParam(required = false) String remark) {
        DcLibraryRead dto = new DcLibraryRead().setKnowledgeId(knowledgeId).setRemark(remark);
        String realName = UserCurrentUtils.getCurrentUser().getRealName();
        dto.setName(realName);
        service.save(dto);

        return R.ok(dto);
    }

}
