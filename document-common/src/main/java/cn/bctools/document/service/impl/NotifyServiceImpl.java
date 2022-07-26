package cn.bctools.document.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.service.DcLibraryService;
import cn.bctools.document.service.DcLibraryUserService;
import cn.bctools.document.service.NotifyService;
import cn.bctools.redis.utils.RedisUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: ZhuXiaoKang
 * @Description:
 */
@Slf4j
@Service
@AllArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    RedisUtils redisUtils;
    DcLibraryService dcLibraryService;
    DcLibraryUserService dcLibraryUserService;
    UserComponent userComponent;

    @Async
    @Override
    public void autoSendReadNotify(String userId, String id) {
        long sendTimeMillis = System.currentTimeMillis();
        // knowledge:readNotify:id:userId
        String key = new StringBuilder("knowledge:readNotify:").append(id).append(":").append(userId).toString();
        if (redisUtils.exists(key)) {
            // key存在，表示当天已发送过通知，不再发送
            return;
        }

        // 准备发送查看提醒
        DcLibrary dcLibrary = dcLibraryService.getById(id);
        if (dcLibrary == null || Boolean.FALSE.equals(dcLibrary.getReadNotify())) {
            // 文档不存在，或未开启自动发送查看提醒，则不发送
            return;
        }
        if (DcLibraryTypeEnum.knowledge.equals(dcLibrary.getType()) || DcLibraryTypeEnum.directory.equals(dcLibrary.getType())) {
            // 非文档变更，不发通知
            return;
        }
        // 获取知识库
        DcLibrary knowledge = dcLibraryService.getOne(Wrappers.<DcLibrary>lambdaQuery()
                .eq(DcLibrary::getId, dcLibrary.getKnowledgeId())
                .select(DcLibrary::getId, DcLibrary::getReadNotify));
        if (knowledge == null || Boolean.FALSE.equals(knowledge.getReadNotify())) {
            // 知识库不存在，或知识库未开启自动发送查看提醒，则不发送
            return;
        }


        // 封装“查看提醒”通知IM数据
        // 得到要通知的用户（知识库所有成员，不包括当前操作人）
        List<DcLibraryUser> dcLibraryUsers = dcLibraryUserService.list(Wrappers.<DcLibraryUser>lambdaQuery().eq(DcLibraryUser::getDcLibraryId, knowledge.getId()).select(DcLibraryUser::getUserId));
        List<String> toUserIds = Optional.ofNullable(dcLibraryUsers).map(users -> users.stream().filter(user -> !user.getUserId().equals(userId)).map(DcLibraryUser::getUserId).collect(Collectors.toList())).orElse(Collections.emptyList());
        if (CollectionUtils.isEmpty(toUserIds)) {
            log.info("没有需要通知的用户");
            return;
        }
        // 通知内容
        Map<String, UserDto> userMap = userComponent.getUserMap(Arrays.asList(userId));
        String content = new StringBuilder().append(Optional.ofNullable(userMap.get(userId).getRealName()).orElse("")).append("提醒你查看《").append(dcLibrary.getName()).append("》").toString();
//        NotifyDto notifyDto = new NotifyDto();
//        // 更多业务类型，见内部文档库《IM接口》说明
//        notifyDto.setBusinessType("dc_read");
//        notifyDto.setCreateTime(sendTimeMillis);
//        notifyDto.setNotifyType(2);
//        notifyDto.setToUserIds(toUserIds);
//        notifyDto.setFrom(userId);
//        notifyDto.setTitle("文档查看");
//        JSONObject jsonContent = new JSONObject();
//        jsonContent.put("data", content);
//        notifyDto.setContent(jsonContent);
//        // 发送IM通知
//        if(imComponent.readNotify(notifyDto)) {
//            // 发送成功，则缓存当天
//            LocalDateTime t = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);
//            long until = ChronoUnit.SECONDS.between(LocalDateTime.now(), t);
//            RedisUtil.set(key, "1", until, TimeUnit.SECONDS);
//        }
    }
}
