package kr.co.ultari.at_board.dto.notification;

import lombok.Builder;
import lombok.Getter;

import java.util.Collection;

@Getter
@Builder
public class MsgNotiRequestDto {
    private String notiCode;
    private String senderName;
    private String systemCode;
    private String notiTitle;
    private String notiContents;
    private String linkUrl;
    private String sendDate;
    private String alertType;
    private boolean persistence;
    private Collection<NotiReceiverVO> receivers;
}