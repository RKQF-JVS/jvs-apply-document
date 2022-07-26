package cn.bctools.document.component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.bctools.common.exception.BusinessException;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.entity.enums.DcLibraryUserRoleEnum;
import cn.bctools.document.enums.OperationEnum;
import cn.bctools.document.service.DcLibraryService;
import cn.bctools.document.service.DcLibraryUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库操作权限校验
 */

@Slf4j
@Component
public class RoleComponent {
    @Autowired
    private DcLibraryService dcLibraryService;
    @Autowired
    private DcLibraryUserService dcLibraryUserService;

    /**
     * 操作权限校验
     *
     * @param operationEnum 操作类型
     * @param userId        用户id
     * @param documentId    知识库id、目录id、文档id
     */
    public void checkOperationAuthority(OperationEnum operationEnum, String userId, String documentId) {
        switch (operationEnum) {
            // 1. 知识库所有者或管理员有权限
            case DC_LIBRARY_ADD:
                // 新增知识库目录、文档
            case DC_LIBRARY_RENAME:
                // 重命名
            case READ_NOTIFY_SETTING:
                // 查看提醒
            case DC_LIBRARY_DOCUMENT_SAVE:
                // 保存文档
                checkRoleOwnerOrAdmin(userId, documentId);
                break;

            // 2. 所有者有权限
            case DC_LIBRARY_SETTING:
                // 知识库设置
            case MEMBER:
                // 知识库人员设置
            case SHARE_SETTING:
                // 分享
                checkRoleOwner(userId, documentId);
                break;

            // 3. 自定义权限判断
            case DC_LIBRARY_DEL:
                // 删除知识库(知识库、目录、文档)权限校验
                delDcLibraryRole(userId, documentId);
                break;
            default:
                throw new BusinessException("无权操作");
        }
    }

    /**
     * 查询用户知识库权限
     *
     * @param userId     用户id
     * @param documentId 知识库id(可以是知识库id、目录id、文档id。最终都会以知识库id查询权限)
     */
    private DcLibraryUser getUserRole(String userId, String documentId) {
        if (StringUtils.isBlank(userId) && StringUtils.isBlank(documentId)) {
            log.error("权限校验入参为空：userId:{}, documentId: {}", userId, documentId);
            throw new BusinessException("无权操作");
        }
        // 查询知识库
        DcLibrary dcLibrary = dcLibraryService.getKnowledgeByChildren(documentId);
        if (dcLibrary == null) {
            throw new BusinessException("知识库不存在");
        }
        // 拥有者才有权限
        return dcLibraryUserService.getOne(Wrappers.<DcLibraryUser>lambdaQuery()
                .eq(DcLibraryUser::getUserId, userId)
                .eq(DcLibraryUser::getDcLibraryId, dcLibrary.getId())
                .select(DcLibraryUser::getRole));
    }

    /**
     * 权限校验——知识库“拥有者”有权操作
     *
     * @param userId     用户id
     * @param documentId 知识库id(可以是知识库id、目录id、文档id。最终都会以知识库id查询权限)
     */
    private void checkRoleOwner(String userId, String documentId) {
        DcLibraryUser dcLibraryUser = getUserRole(userId, documentId);
        if (dcLibraryUser == null || !DcLibraryUserRoleEnum.owner.equals(dcLibraryUser.getRole())) {
            throw new BusinessException("知识库所有者有权操作");
        }
    }

    /**
     * 权限校验——知识库“拥有者” 或 “管理员”有权操作
     *
     * @param userId     用户id
     * @param documentId 知识库id(可以是知识库id、目录id、文档id。最终都会以知识库id查询权限)
     */
    private void checkRoleOwnerOrAdmin(String userId, String documentId) {
        DcLibraryUser dcLibraryUser = getUserRole(userId, documentId);
        boolean flag = dcLibraryUser == null ||
                (!DcLibraryUserRoleEnum.owner.equals(dcLibraryUser.getRole()) && !DcLibraryUserRoleEnum.admin.equals(dcLibraryUser.getRole()));
        if (flag) {
            throw new BusinessException("知识库所有者或管理员有权操作");
        }
    }

    /**
     * 删除知识库（知识库、目录、文档）权限校验
     *
     * @param userId
     * @param documentId
     */
    private void delDcLibraryRole(String userId, String documentId) {
        DcLibrary library = dcLibraryService.getById(documentId);
        if (library == null) {
            throw new BusinessException("数据不存在");
        }
        if (library.getType().equals(DcLibraryTypeEnum.knowledge)) {
            // 删除知识库，知识库所有者有权限操作
            checkRoleOwner(userId, documentId);
        } else {
            // 删除目录|文档，知识库所有者或管理员有权限操作
            checkRoleOwnerOrAdmin(userId, documentId);
        }
    }

}
