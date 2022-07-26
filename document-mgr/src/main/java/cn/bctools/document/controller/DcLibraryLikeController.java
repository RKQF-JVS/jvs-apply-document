package cn.bctools.document.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.common.utils.R;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.entity.DcLibraryLike;
import cn.bctools.document.service.DcLibraryLikeService;
import cn.bctools.document.vo.req.DcLibraryLikeVo;
import cn.bctools.document.vo.res.DcLibraryLikeResVo;
import cn.bctools.log.annotation.Log;
import cn.bctools.oauth2.utils.UserCurrentUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库点赞
 */
@Slf4j
@Api(tags = "点赞")
@RestController
@RequestMapping(value = "/dcLibrary/like")
@AllArgsConstructor
public class DcLibraryLikeController {

    private final DcLibraryLikeService dcLibraryLikeService;
    private final UserComponent userComponent;

    @Log
    @ApiOperation("点赞|取消点赞")
    @PostMapping
    public R<DcLibraryLikeResVo> like(@Validated DcLibraryLikeVo likeVo) {
        UserDto currentUser = UserCurrentUtils.getCurrentUser();
        // 查询是否已点赞
        LambdaQueryWrapper<DcLibraryLike> queryWrapper = Wrappers.<DcLibraryLike>lambdaQuery()
                .eq(DcLibraryLike::getUserId, currentUser.getId())
                .eq(DcLibraryLike::getBizResourceId, likeVo.getBizResourceId())
                .eq(DcLibraryLike::getBizType, likeVo.getBizType());
        DcLibraryLike dcLibraryLike = dcLibraryLikeService.getOne(queryWrapper);

        // 已点赞，则取消点赞(删除点赞记录)；未点赞，则保存点赞记录
        if (dcLibraryLike == null) {
            DcLibraryLike like = new DcLibraryLike();
            like.setUserId(currentUser.getId());
            like.setRealName(StringUtils.isNotEmpty(currentUser.getRealName()) ? currentUser.getRealName() : currentUser.getRealName());
            like.setBizType(likeVo.getBizType());
            like.setBizResourceId(likeVo.getBizResourceId());
            dcLibraryLikeService.save(like);

            // 封装响应
            DcLibraryLikeResVo resVo = BeanUtil.copyProperties(like, DcLibraryLikeResVo.class);
            resVo.setHeadImg(currentUser.getHeadImg());
            return R.ok(resVo);
        } else {
            dcLibraryLikeService.removeById(dcLibraryLike.getId());
            return R.ok();
        }
    }

    @Log
    @ApiOperation("点赞人员列表")
    @GetMapping
    public R<List<DcLibraryLikeResVo>> likeMembers(@Validated DcLibraryLikeVo likeVo) {
        List<DcLibraryLikeResVo> resVos = null;
        // 查询点赞人员列表
        LambdaQueryWrapper<DcLibraryLike> queryWrapper = Wrappers.<DcLibraryLike>lambdaQuery()
                .eq(DcLibraryLike::getBizResourceId, likeVo.getBizResourceId())
                .eq(DcLibraryLike::getBizType, likeVo.getBizType())
                .orderByAsc(DcLibraryLike::getCreateTime);
        List<DcLibraryLike> likeMemberList = dcLibraryLikeService.list(queryWrapper);
        if (CollectionUtils.isEmpty(likeMemberList)) {
            return R.ok(Collections.emptyList());
        }

        // 获取头像
        List<String> userIds = likeMemberList.stream().map(DcLibraryLike::getUserId).collect(Collectors.toList());
        Map<String, UserDto> userMap = userComponent.getUserMap(userIds);
        resVos = likeMemberList.stream().map(e -> {
            DcLibraryLikeResVo resVo = BeanUtil.copyProperties(e, DcLibraryLikeResVo.class);
            resVo.setHeadImg(userMap.get(e.getUserId()).getHeadImg());
            return resVo;
        }).collect(Collectors.toList());
        return R.ok(resVos);
    }


}
