package cn.bctools.document.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.common.utils.R;
import cn.bctools.document.component.RoleComponent;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.dto.DcUserDto;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.entity.enums.DcLibraryUserRoleEnum;
import cn.bctools.document.enums.OperationEnum;
import cn.bctools.document.service.DcLibraryService;
import cn.bctools.document.service.DcLibraryUserService;
import cn.bctools.document.vo.BaseReqVo;
import cn.bctools.document.vo.res.DcLibraryMemberResVo;
import cn.bctools.log.annotation.Log;
import cn.bctools.oauth2.utils.UserCurrentUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库成员
 */

@Slf4j
@Api(tags = "知识库成员")
@RestController
@RequestMapping(value = "/dcLibrary")
@AllArgsConstructor
public class DcLibraryUserController {

    private final DcLibraryUserService dcLibraryUserService;
    private final RoleComponent roleComponent;
    private final DcLibraryService dcLibraryService;
    private final UserComponent userComponent;

    @SuppressWarnings("rawtypes")
    @Log
    @PutMapping("/add/member")
    @ApiOperation("添加知识库成员")
    public R<String> putUser(@RequestBody @Validated DcUserDto dcUserDto) {
        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.MEMBER, UserCurrentUtils.getUserId(), dcUserDto.getDocumentId());
        // 添加成员
        List<String> userIds = dcUserDto.getUserIds();
        if (ObjectUtil.isNotEmpty(userIds)) {
            Map<String, UserDto> userMap = userComponent.getUserMap(userIds);
            List<DcLibraryUser> dcLibraryUsers = userMap.values().stream().map(e -> new DcLibraryUser().setUserId(e.getId()).setRealName(e.getRealName()).setDcLibraryId(dcUserDto.getDocumentId())).collect(Collectors.toList());
            dcLibraryService.putUser(dcUserDto.getDocumentId(), dcLibraryUsers);
        }
        return R.ok();
    }

    @Log
    @GetMapping("/query/member/{id}")
    @ApiOperation("查询知识库成员")
    public R<Page<DcLibraryMemberResVo>> queryUser(Page<DcLibraryUser> page, @PathVariable String id, BaseReqVo reqVo) {
        Page<DcLibraryMemberResVo> resPage = new Page<>(page.getCurrent(), page.getSize());

        String userId = UserCurrentUtils.getUserId();
        Page<DcLibraryUser> userPage = dcLibraryService.queryUser(page, id, userId);

        if (CollectionUtils.isNotEmpty(userPage.getRecords())) {
            List<String> userIds = userPage.getRecords().stream().map(DcLibraryUser::getUserId).collect(Collectors.toList());
            // 查询用户头像
            Map<String, UserDto> userMap = userComponent.getUserMap(userIds);
            List<DcLibraryMemberResVo> resVos = userPage.getRecords().stream()
                    .filter(user -> ObjectUtil.isNotEmpty(userMap.get(user.getUserId())))
                    .map(e -> {
                        DcLibraryMemberResVo resVo = BeanUtil.copyProperties(e, DcLibraryMemberResVo.class);
                        resVo.setHeadImg(userMap.get(e.getUserId()).getHeadImg());
                        resVo.setRealName(userMap.get(e.getUserId()).getRealName());
                        return resVo;
                    }).collect(Collectors.toList());

            resPage.setTotal(userPage.getTotal());
            resPage.setRecords(resVos);
        }

        return R.ok(resPage);
    }

    @Log
    @PutMapping("/modify/role/{documentId}/{userId}/{role}")
    @ApiOperation("知识库成员角色修改")
    public R<String> modifyRoleById(@PathVariable("documentId") String documentId, @PathVariable("userId") String userId, @PathVariable("role") DcLibraryUserRoleEnum role) {
        if (role == DcLibraryUserRoleEnum.owner) {
            return R.failed("暂不支持设置其他人为文档所有者");
        }

        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.MEMBER, UserCurrentUtils.getUserId(), documentId);

        dcLibraryUserService.update(Wrappers.<DcLibraryUser>lambdaUpdate().eq(DcLibraryUser::getDcLibraryId, documentId)
                .eq(DcLibraryUser::getUserId, userId)
                //不更新所有者的角色
                .ne(DcLibraryUser::getRole, DcLibraryUserRoleEnum.owner)
                .set(DcLibraryUser::getRole, role));
        return R.ok();
    }

    @Log
    @DeleteMapping("/delete/member/{documentId}/{userId}")
    @ApiOperation("删除知识库成员")
    public R<String> deleteMemberById(@PathVariable("documentId") String documentId, @PathVariable("userId") String userId) {
        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.MEMBER, UserCurrentUtils.getUserId(), documentId);
        dcLibraryService.deleteMemberById(documentId, userId);
        return R.ok();
    }
}
