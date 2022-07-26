package cn.bctools.document.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.common.utils.R;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.entity.DcLibraryComment;
import cn.bctools.document.entity.DcLibraryLike;
import cn.bctools.document.entity.enums.DcLibraryLikeTypeEnum;
import cn.bctools.document.service.DcLibraryCommentService;
import cn.bctools.document.service.DcLibraryLikeService;
import cn.bctools.document.vo.DcLibraryCommentVo;
import cn.bctools.document.vo.req.DcCommentSaveReqVo;
import cn.bctools.log.annotation.Log;
import cn.bctools.log.enums.OperationType;
import cn.bctools.oauth2.utils.UserCurrentUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Desc 知识库-评论
 **/
@Slf4j
@Api(tags = "评论")
@RestController
@AllArgsConstructor
@RequestMapping("/dcLibrary/comment")
public class DcLibraryCommentController {

    private final DcLibraryCommentService service;
    private final DcLibraryLikeService dcLibraryLikeService;
    private final UserComponent userComponent;

    @Log
    @ApiOperation("分页")
    @GetMapping("/page/{knowledgeId}")
    public R<Page<DcLibraryCommentVo>> page(Page<DcLibraryComment> page,
                                            @ApiParam(value = "知识库id", required = true) @PathVariable("knowledgeId") String knowledgeId,
                                            @ApiParam("上级留言id") String parentId) {

        // 分页查询
        service.page(page, Wrappers.<DcLibraryComment>lambdaQuery()
                .eq(DcLibraryComment::getKnowledgeId, knowledgeId)
                .isNull(StrUtil.isEmpty(parentId), DcLibraryComment::getParentId)
                .eq(StrUtil.isNotEmpty(parentId), DcLibraryComment::getParentId, parentId)
                .orderByDesc(DcLibraryComment::getCreateTime)
        );

        // 封装响应
        List<String> userIds = new ArrayList<>();
        List<DcLibraryCommentVo> result = page.getRecords().stream().map(e -> {
            DcLibraryCommentVo vo = BeanUtil.copyProperties(e, DcLibraryCommentVo.class);
            userIds.add(vo.getUserId());
            if (service.lambdaQuery().eq(DcLibraryComment::getKnowledgeId, e.getKnowledgeId()).eq(DcLibraryComment::getParentId, e.getId()).count() >= 1) {
                vo.setChildren(true);
            }
            // 封装点赞信息
            LambdaQueryWrapper<DcLibraryLike> likeCountQuery = Wrappers.<DcLibraryLike>lambdaQuery()
                    .eq(DcLibraryLike::getBizResourceId, e.getId())
                    .eq(DcLibraryLike::getBizType, DcLibraryLikeTypeEnum.COMMENT);
            List<DcLibraryLike> likes = dcLibraryLikeService.list(likeCountQuery);
            vo.setLikeTotal(likes.size());
            vo.setCurrentUserLike(likes.stream().anyMatch(l -> l.getUserId().equals(UserCurrentUtils.getUserId())));
            return vo;
        }).collect(Collectors.toList());

        // 查询用户头像
        Map<String, UserDto> userMap = userComponent.getUserMap(userIds);
        result.stream().forEach(r -> r.setHeadImg(userMap.get(r.getUserId()).getHeadImg()));
        return R.ok(new Page<DcLibraryCommentVo>().setTotal(page.getTotal()).setCurrent(page.getCurrent()).setSize(page.getSize()).setRecords(result));
    }

    @Log(operationType = OperationType.ADD)
    @ApiOperation("新增")
    @PostMapping("/save")
    public R<DcLibraryComment> save(@RequestBody @Validated DcCommentSaveReqVo reqVo) {
        UserDto userDto = UserCurrentUtils.getCurrentUser();
        DcLibraryComment dto = new DcLibraryComment().setKnowledgeId(reqVo.getKnowledgeId()).setMessage(reqVo.getMessage()).setParentId(reqVo.getParentId());
        dto.setName(StringUtils.isNotBlank(userDto.getRealName()) ? userDto.getRealName() : userDto.getRealName());
        dto.setUserId(userDto.getId());
        service.save(dto);
        return R.ok(dto);
    }

    @Log(operationType = OperationType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/del/{id}")
    public R<Boolean> remove(@PathVariable("id") String id) {
        boolean data = service.removeById(id);
        return R.ok(data);
    }

}
