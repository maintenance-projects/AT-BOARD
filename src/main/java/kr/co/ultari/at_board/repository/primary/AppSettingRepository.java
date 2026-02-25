package kr.co.ultari.at_board.repository.primary;

import kr.co.ultari.at_board.model.primary.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
