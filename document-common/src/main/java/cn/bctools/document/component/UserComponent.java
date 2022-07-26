package cn.bctools.document.component;

import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.oauth2.utils.AuthorityManagementUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 用户相关组件 API调用
 */

@Slf4j
@Component
public class UserComponent {

    /**
     * 获取用户头像map
     *
     * @param userIds 用户id集合
     */
    public Map<String, UserDto> getUserMap(List<String> userIds) {
        Map<String, UserDto> userMap = AuthorityManagementUtils.getUsersByIds(userIds).stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
        return userMap;
    }


}
