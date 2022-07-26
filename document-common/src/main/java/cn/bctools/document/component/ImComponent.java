//package cn.bctools.document.component;
//
//import cn.bctools.im.api.ImServiceApi;
//import cn.bctools.im.dto.NotifyDto;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
///**
// * @Author: ZhuXiaoKang
// * @Description: IM
// */
//@Slf4j
//@Component
//public class ImComponent {
//
////    @Autowired
////    private ImServiceApi imServiceApi;
//
//    /**
//     * 发送查看提醒
//     * @param notifyDto 通知实体
//     * @return 发送结果
//     */
//    public boolean readNotify(NotifyDto notifyDto) {
//        try {
////            imServiceApi.notify(notifyDto);
//            log.info("im通知发送成功");
//            return Boolean.TRUE;
//        } catch (Exception e) {
//            log.error("im通知发送异常", e);
//            return Boolean.FALSE;
//        }
//    }
//}
