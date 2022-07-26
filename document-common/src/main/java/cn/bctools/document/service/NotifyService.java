package cn.bctools.document.service;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-通知服务
 */
public interface NotifyService {

    /**
     * 自动发送查看提醒
     * @param userId 操作人id
     * @param id 文档id
     */
    void autoSendReadNotify(String userId, String id);
}
