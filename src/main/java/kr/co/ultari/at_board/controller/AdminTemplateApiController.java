package kr.co.ultari.at_board.controller;

import kr.co.ultari.at_board.model.primary.Admin;
import kr.co.ultari.at_board.service.BoardTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/templates")
@RequiredArgsConstructor
public class AdminTemplateApiController {

    private final BoardTemplateService boardTemplateService;

    private Admin getAdmin(HttpSession session) {
        return (Admin) session.getAttribute("adminUser");
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", "인증이 필요합니다.");
        return ResponseEntity.status(401).body(r);
    }

    // ── 트리 조회 ─────────────────────────────────────

    @GetMapping("/tree")
    public ResponseEntity<?> getTree(HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        return ResponseEntity.ok(boardTemplateService.getAllSectionsWithTemplates());
    }

    // ── 양식 단건 조회 ────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        Map<String, Object> tpl = boardTemplateService.getTemplateForApi(id);
        if (tpl == null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("message", "양식을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(r);
        }
        return ResponseEntity.ok(tpl);
    }

    // ── 양식 생성 ─────────────────────────────────────

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> body, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("message", "양식명을 입력하세요.");
            return ResponseEntity.badRequest().body(r);
        }
        Long sectionId = body.get("sectionId") != null ? Long.valueOf(body.get("sectionId").toString()) : null;
        return ResponseEntity.ok(boardTemplateService.createTemplate(sectionId, name));
    }

    // ── 양식 수정 ─────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(@PathVariable Long id,
                                                               @RequestBody Map<String, Object> body,
                                                               HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        String name = (String) body.get("name");
        String content = (String) body.get("content");
        Boolean isActive = body.get("isActive") != null ? Boolean.valueOf(body.get("isActive").toString()) : null;
        Long sectionId = null;
        if (body.containsKey("sectionId")) {
            Object sid = body.get("sectionId");
            sectionId = sid != null ? Long.valueOf(sid.toString()) : -1L; // null → -1 (섹션 없음)
        }
        return ResponseEntity.ok(boardTemplateService.updateTemplate(id, name, content, isActive, sectionId));
    }

    // ── 양식 삭제 ─────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        return ResponseEntity.ok(boardTemplateService.deleteTemplate(id));
    }

    // ── 양식 섹션 이동 ────────────────────────────────

    @PostMapping("/{id}/move-section")
    public ResponseEntity<Map<String, Object>> moveToSection(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> body,
                                                              HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        Object sid = body.get("sectionId");
        Long sectionId = sid != null ? Long.valueOf(sid.toString()) : -1L;
        return ResponseEntity.ok(boardTemplateService.moveTemplateToSection(id, sectionId));
    }

    // ── 양식 순서 이동 ────────────────────────────────

    @PostMapping("/{id}/up")
    public ResponseEntity<Map<String, Object>> moveTemplateUp(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", boardTemplateService.moveTemplateUp(id));
        return ResponseEntity.ok(r);
    }

    @PostMapping("/{id}/down")
    public ResponseEntity<Map<String, Object>> moveTemplateDown(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", boardTemplateService.moveTemplateDown(id));
        return ResponseEntity.ok(r);
    }

    // ── 섹션 생성 ─────────────────────────────────────

    @PostMapping("/sections")
    public ResponseEntity<Map<String, Object>> createSection(@RequestBody Map<String, Object> body, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("message", "섹션명을 입력하세요.");
            return ResponseEntity.badRequest().body(r);
        }
        return ResponseEntity.ok(boardTemplateService.createSection(name));
    }

    // ── 섹션 수정 ─────────────────────────────────────

    @PutMapping("/sections/{id}")
    public ResponseEntity<Map<String, Object>> renameSection(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> body,
                                                              HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("message", "섹션명을 입력하세요.");
            return ResponseEntity.badRequest().body(r);
        }
        return ResponseEntity.ok(boardTemplateService.renameSection(id, name));
    }

    // ── 섹션 삭제 ─────────────────────────────────────

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Map<String, Object>> deleteSection(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        return ResponseEntity.ok(boardTemplateService.deleteSection(id));
    }

    // ── 섹션 순서 이동 ────────────────────────────────

    @PostMapping("/sections/{id}/up")
    public ResponseEntity<Map<String, Object>> moveSectionUp(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", boardTemplateService.moveSectionUp(id));
        return ResponseEntity.ok(r);
    }

    @PostMapping("/sections/{id}/down")
    public ResponseEntity<Map<String, Object>> moveSectionDown(@PathVariable Long id, HttpSession session) {
        if (getAdmin(session) == null) return unauthorized();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", boardTemplateService.moveSectionDown(id));
        return ResponseEntity.ok(r);
    }
}
