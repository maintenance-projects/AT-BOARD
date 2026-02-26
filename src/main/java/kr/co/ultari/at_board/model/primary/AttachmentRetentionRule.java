package kr.co.ultari.at_board.model.primary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRetentionRule {
    /** 이 크기(MB) 이상의 파일에 적용 */
    private int sizeThresholdMb;
    /** 보관 기간 (일), 0 이하 = 무기한 */
    private int retentionDays;
}
