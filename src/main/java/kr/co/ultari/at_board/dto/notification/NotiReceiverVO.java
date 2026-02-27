package kr.co.ultari.at_board.dto.notification;

import lombok.Builder;
import lombok.Getter;

import java.util.Collection;

@Getter
@Builder
public class NotiReceiverVO {
    private Collection<String> list;
    private NotiReceiverType type;
}