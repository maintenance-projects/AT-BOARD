package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class AdminSettingController {

    private final AppSettingService appSettingService;

    @Value("${spring.servlet.multipart.max-file-size:50MB}")
    private String multipartMaxFileSizeStr;

    /** spring.servlet.multipart.max-file-size 값을 MB 단위로 반환 */
    private long getMultipartMaxSizeMb() {
        String val = multipartMaxFileSizeStr.trim().toUpperCase();
        if (val.endsWith("MB")) {
            return Long.parseLong(val.substring(0, val.length() - 2).trim());
        } else if (val.endsWith("GB")) {
            return Long.parseLong(val.substring(0, val.length() - 2).trim()) * 1024;
        } else if (val.endsWith("KB")) {
            return Long.parseLong(val.substring(0, val.length() - 2).trim()) / 1024;
        }
        return Long.parseLong(val) / (1024L * 1024);
    }

    @GetMapping
    public String settingsPage(Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        long maxSizeBytes = appSettingService.getMaxAttachmentSize();
        model.addAttribute("admin", admin);
        model.addAttribute("maxSizeMb", maxSizeBytes / (1024 * 1024));
        model.addAttribute("multipartMaxSizeMb", getMultipartMaxSizeMb());
        model.addAttribute("blockedExtensions", appSettingService.getBlockedExtensions());
        model.addAttribute("retentionRules", appSettingService.getRetentionRules());
        return "pages/admin/settings";
    }

    @PostMapping("/max-size")
    public String updateMaxSize(@RequestParam long maxSizeMb,
                                RedirectAttributes ra,
                                HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        if (maxSizeMb <= 0) {
            ra.addFlashAttribute("error", "용량은 1MB 이상이어야 합니다.");
            return "redirect:/admin/settings";
        }

        long multipartMaxSizeMb = getMultipartMaxSizeMb();
        if (maxSizeMb > multipartMaxSizeMb) {
            ra.addFlashAttribute("error",
                "설정할 수 있는 최대 용량은 " + multipartMaxSizeMb + "MB입니다. (서버 multipart 설정 한도)");
            return "redirect:/admin/settings";
        }

        appSettingService.setMaxAttachmentSize(maxSizeMb * 1024 * 1024);
        log.info("Admin {} updated max attachment size to {}MB", admin.getAdminId(), maxSizeMb);
        ra.addFlashAttribute("success", "최대 파일 크기가 " + maxSizeMb + "MB로 설정되었습니다.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/blocked-extensions/add")
    public String addExtension(@RequestParam String extension,
                               RedirectAttributes ra,
                               HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        String cleaned = extension.trim().replaceAll("^\\.", "").toLowerCase();
        if (cleaned.isEmpty()) {
            ra.addFlashAttribute("error", "확장자를 입력하세요.");
            return "redirect:/admin/settings";
        }

        appSettingService.addBlockedExtension(cleaned);
        log.info("Admin {} added blocked extension: {}", admin.getAdminId(), cleaned);
        ra.addFlashAttribute("success", "." + cleaned + " 확장자가 차단 목록에 추가되었습니다.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/blocked-extensions/remove")
    public String removeExtension(@RequestParam String extension,
                                  RedirectAttributes ra,
                                  HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        String cleaned = extension.trim().replaceAll("^\\.", "").toLowerCase();
        appSettingService.removeBlockedExtension(cleaned);
        log.info("Admin {} removed blocked extension: {}", admin.getAdminId(), cleaned);
        ra.addFlashAttribute("success", "." + cleaned + " 확장자가 차단 목록에서 제거되었습니다.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/retention-rules/add")
    public String addRetentionRule(@RequestParam int sizeThresholdMb,
                                   @RequestParam int retentionDays,
                                   RedirectAttributes ra,
                                   HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        if (sizeThresholdMb < 0) {
            ra.addFlashAttribute("error", "파일 크기 기준은 0 이상이어야 합니다.");
            return "redirect:/admin/settings";
        }
        if (retentionDays < 1) {
            ra.addFlashAttribute("error", "보관 기간은 1일 이상이어야 합니다.");
            return "redirect:/admin/settings";
        }

        appSettingService.addRetentionRule(sizeThresholdMb, retentionDays);
        log.info("Admin {} added retention rule: {}MB 이상 → {}일", admin.getAdminId(), sizeThresholdMb, retentionDays);
        ra.addFlashAttribute("success", sizeThresholdMb + "MB 이상 파일 보관 기간이 " + retentionDays + "일로 설정되었습니다.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/retention-rules/remove")
    public String removeRetentionRule(@RequestParam int sizeThresholdMb,
                                      RedirectAttributes ra,
                                      HttpSession session) {
        Admin admin = (Admin) session.getAttribute("adminUser");
        if (admin == null) return "redirect:/admin/login";

        appSettingService.removeRetentionRule(sizeThresholdMb);
        log.info("Admin {} removed retention rule for {}MB", admin.getAdminId(), sizeThresholdMb);
        ra.addFlashAttribute("success", "보관 기간 규칙이 삭제되었습니다.");
        return "redirect:/admin/settings";
    }
}
