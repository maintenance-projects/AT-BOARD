package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardTemplate;
import kr.co.ultari.at_board.model.primary.BoardTemplateSection;
import kr.co.ultari.at_board.repository.primary.BoardTemplateRepository;
import kr.co.ultari.at_board.repository.primary.BoardTemplateSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardTemplateService {

    private final BoardTemplateRepository boardTemplateRepository;
    private final BoardTemplateSectionRepository boardTemplateSectionRepository;

    // ── 글쓰기 화면용 ────────────────────────────────────────

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public List<BoardTemplate> getActiveTemplates() {
        return boardTemplateRepository.findActiveTemplatesOrdered();
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Map<String, List<BoardTemplate>> getActiveTemplatesGrouped() {
        List<BoardTemplate> templates = boardTemplateRepository.findActiveTemplatesOrdered();
        LinkedHashMap<String, List<BoardTemplate>> grouped = new LinkedHashMap<>();
        List<BoardTemplate> unsectioned = new ArrayList<>();
        for (BoardTemplate tpl : templates) {
            BoardTemplateSection sec = tpl.getSection();
            if (sec != null && sec.getName() != null && !sec.getName().trim().isEmpty()) {
                String sectionName = sec.getName();
                if (!grouped.containsKey(sectionName)) {
                    grouped.put(sectionName, new ArrayList<BoardTemplate>());
                }
                grouped.get(sectionName).add(tpl);
            } else {
                unsectioned.add(tpl);
            }
        }
        if (!unsectioned.isEmpty()) {
            grouped.put("", unsectioned);
        }
        return grouped;
    }

    // ── 관리자 API용 ────────────────────────────────────────

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public List<Map<String, Object>> getAllSectionsWithTemplates() {
        List<BoardTemplateSection> sections = boardTemplateSectionRepository.findAllByOrderBySortOrderAscIdAsc();
        List<BoardTemplate> allTemplates = boardTemplateRepository.findAllOrderedWithSection();

        List<Map<String, Object>> result = new ArrayList<>();
        for (BoardTemplateSection sec : sections) {
            Map<String, Object> secMap = new LinkedHashMap<>();
            secMap.put("id", sec.getId());
            secMap.put("name", sec.getName());
            secMap.put("sortOrder", sec.getSortOrder());
            List<Map<String, Object>> tplList = new ArrayList<>();
            for (BoardTemplate tpl : allTemplates) {
                if (tpl.getSection() != null && tpl.getSection().getId().equals(sec.getId())) {
                    tplList.add(toTemplateMap(tpl));
                }
            }
            secMap.put("templates", tplList);
            result.add(secMap);
        }

        // 섹션 없는 양식
        List<Map<String, Object>> unsectionedList = new ArrayList<>();
        for (BoardTemplate tpl : allTemplates) {
            if (tpl.getSection() == null) {
                unsectionedList.add(toTemplateMap(tpl));
            }
        }
        if (!unsectionedList.isEmpty()) {
            Map<String, Object> unsecMap = new LinkedHashMap<>();
            unsecMap.put("id", null);
            unsecMap.put("name", "(섹션 없음)");
            unsecMap.put("sortOrder", 9999);
            unsecMap.put("templates", unsectionedList);
            result.add(unsecMap);
        }

        return result;
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Map<String, Object> getTemplateForApi(Long id) {
        BoardTemplate tpl = boardTemplateRepository.findById(id).orElse(null);
        if (tpl == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tpl.getId());
        map.put("name", tpl.getName());
        map.put("content", tpl.getContent() != null ? tpl.getContent() : "");
        map.put("isActive", tpl.getIsActive());
        map.put("sectionId", tpl.getSection() != null ? tpl.getSection().getId() : null);
        return map;
    }

    // ── 섹션 CRUD ────────────────────────────────────────

    @Transactional("primaryTransactionManager")
    public Map<String, Object> createSection(String name) {
        List<BoardTemplateSection> all = boardTemplateSectionRepository.findAllByOrderBySortOrderAscIdAsc();
        int nextOrder = all.size() + 1;
        BoardTemplateSection sec = BoardTemplateSection.builder()
                .name(name.trim())
                .sortOrder(nextOrder)
                .build();
        sec = boardTemplateSectionRepository.save(sec);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", sec.getId());
        return result;
    }

    @Transactional("primaryTransactionManager")
    public Map<String, Object> renameSection(Long id, String name) {
        Map<String, Object> result = new LinkedHashMap<>();
        BoardTemplateSection sec = boardTemplateSectionRepository.findById(id).orElse(null);
        if (sec == null) {
            result.put("success", false);
            result.put("message", "섹션을 찾을 수 없습니다.");
            return result;
        }
        sec.setName(name.trim());
        boardTemplateSectionRepository.save(sec);
        result.put("success", true);
        return result;
    }

    @Transactional("primaryTransactionManager")
    public Map<String, Object> deleteSection(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        BoardTemplateSection sec = boardTemplateSectionRepository.findById(id).orElse(null);
        if (sec == null) {
            result.put("success", false);
            result.put("message", "섹션을 찾을 수 없습니다.");
            return result;
        }
        if (boardTemplateRepository.existsBySection(sec)) {
            result.put("success", false);
            result.put("message", "해당 섹션에 양식이 있어 삭제할 수 없습니다. 먼저 양식을 삭제하거나 이동하세요.");
            return result;
        }
        boardTemplateSectionRepository.deleteById(id);
        result.put("success", true);
        return result;
    }

    @Transactional("primaryTransactionManager")
    public boolean moveSectionUp(Long id) {
        List<BoardTemplateSection> all = boardTemplateSectionRepository.findAllByOrderBySortOrderAscIdAsc();
        return moveSectionInList(all, id, -1);
    }

    @Transactional("primaryTransactionManager")
    public boolean moveSectionDown(Long id) {
        List<BoardTemplateSection> all = boardTemplateSectionRepository.findAllByOrderBySortOrderAscIdAsc();
        return moveSectionInList(all, id, 1);
    }

    private boolean moveSectionInList(List<BoardTemplateSection> all, Long id, int direction) {
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) { idx = i; break; }
        }
        int swapIdx = idx + direction;
        if (idx < 0 || swapIdx < 0 || swapIdx >= all.size()) return false;

        // 메모리에서 normalize + swap 후 saveAll 한 번
        for (int i = 0; i < all.size(); i++) {
            all.get(i).setSortOrder(i + 1);
        }
        int tmp = all.get(idx).getSortOrder();
        all.get(idx).setSortOrder(all.get(swapIdx).getSortOrder());
        all.get(swapIdx).setSortOrder(tmp);
        boardTemplateSectionRepository.saveAll(all);
        return true;
    }

    // ── 양식 CRUD ────────────────────────────────────────

    @Transactional("primaryTransactionManager")
    public Map<String, Object> createTemplate(Long sectionId, String name) {
        BoardTemplateSection section = null;
        if (sectionId != null) {
            section = boardTemplateSectionRepository.findById(sectionId).orElse(null);
        }
        List<BoardTemplate> siblings = section != null
                ? boardTemplateRepository.findBySectionOrderBySortOrderAscNameAsc(section)
                : boardTemplateRepository.findBySectionIsNullOrderBySortOrderAscNameAsc();
        int nextOrder = siblings.size() + 1;

        BoardTemplate tpl = BoardTemplate.builder()
                .name(name.trim())
                .content("")
                .section(section)
                .isActive(Boolean.TRUE)
                .sortOrder(nextOrder)
                .build();
        tpl = boardTemplateRepository.save(tpl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", tpl.getId());
        return result;
    }

    @Transactional("primaryTransactionManager")
    public Map<String, Object> updateTemplate(Long id, String name, String content, Boolean isActive, Long sectionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        BoardTemplate tpl = boardTemplateRepository.findById(id).orElse(null);
        if (tpl == null) {
            result.put("success", false);
            result.put("message", "양식을 찾을 수 없습니다.");
            return result;
        }
        if (name != null) tpl.setName(name.trim());
        tpl.setContent(content != null ? content : "");
        tpl.setIsActive(isActive != null ? isActive : Boolean.TRUE);
        // 섹션 변경 (sectionId == -1 → 섹션 없음, null → 기존 유지, 양수 → 해당 섹션으로 이동)
        if (sectionId != null) {
            if (sectionId < 0) {
                tpl.setSection(null);
            } else {
                BoardTemplateSection section = boardTemplateSectionRepository.findById(sectionId).orElse(null);
                tpl.setSection(section);
            }
        }
        boardTemplateRepository.save(tpl);
        result.put("success", true);
        return result;
    }

    @Transactional("primaryTransactionManager")
    public Map<String, Object> deleteTemplate(Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!boardTemplateRepository.existsById(id)) {
            result.put("success", false);
            result.put("message", "양식을 찾을 수 없습니다.");
            return result;
        }
        boardTemplateRepository.deleteById(id);
        result.put("success", true);
        return result;
    }

    @Transactional("primaryTransactionManager")
    public Map<String, Object> moveTemplateToSection(Long id, Long newSectionId) {
        Map<String, Object> result = new LinkedHashMap<>();
        BoardTemplate tpl = boardTemplateRepository.findById(id).orElse(null);
        if (tpl == null) { result.put("success", false); result.put("message", "양식을 찾을 수 없습니다."); return result; }

        BoardTemplateSection newSection = null;
        if (newSectionId != null && newSectionId > 0) {
            newSection = boardTemplateSectionRepository.findById(newSectionId).orElse(null);
        }
        tpl.setSection(newSection);

        // 새 섹션 내 맨 뒤 순서로 배치 (sortOrder 중복 방지)
        List<BoardTemplate> siblings = newSection != null
                ? boardTemplateRepository.findBySectionOrderBySortOrderAscNameAsc(newSection)
                : boardTemplateRepository.findBySectionIsNullOrderBySortOrderAscNameAsc();
        // siblings에 tpl 자신이 포함될 수 있으므로 제외
        int count = 0;
        for (BoardTemplate s : siblings) {
            if (!s.getId().equals(id)) count++;
        }
        tpl.setSortOrder(count + 1);

        boardTemplateRepository.save(tpl);
        result.put("success", true);
        return result;
    }

    @Transactional("primaryTransactionManager")
    public boolean moveTemplateUp(Long id) {
        BoardTemplate target = boardTemplateRepository.findById(id).orElse(null);
        if (target == null) return false;
        List<BoardTemplate> siblings = target.getSection() != null
                ? boardTemplateRepository.findBySectionOrderBySortOrderAscNameAsc(target.getSection())
                : boardTemplateRepository.findBySectionIsNullOrderBySortOrderAscNameAsc();
        return moveTemplateInList(siblings, id, -1);
    }

    @Transactional("primaryTransactionManager")
    public boolean moveTemplateDown(Long id) {
        BoardTemplate target = boardTemplateRepository.findById(id).orElse(null);
        if (target == null) return false;
        List<BoardTemplate> siblings = target.getSection() != null
                ? boardTemplateRepository.findBySectionOrderBySortOrderAscNameAsc(target.getSection())
                : boardTemplateRepository.findBySectionIsNullOrderBySortOrderAscNameAsc();
        return moveTemplateInList(siblings, id, 1);
    }

    private boolean moveTemplateInList(List<BoardTemplate> items, Long id, int direction) {
        int idx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) { idx = i; break; }
        }
        int swapIdx = idx + direction;
        if (idx < 0 || swapIdx < 0 || swapIdx >= items.size()) return false;

        // 메모리에서 normalize + swap 후 saveAll 한 번
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortOrder(i + 1);
        }
        int tmp = items.get(idx).getSortOrder();
        items.get(idx).setSortOrder(items.get(swapIdx).getSortOrder());
        items.get(swapIdx).setSortOrder(tmp);
        boardTemplateRepository.saveAll(items);
        return true;
    }

    // ── 헬퍼 ────────────────────────────────────────

    private Map<String, Object> toTemplateMap(BoardTemplate tpl) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tpl.getId());
        map.put("name", tpl.getName());
        map.put("isActive", tpl.getIsActive());
        map.put("sortOrder", tpl.getSortOrder());
        return map;
    }
}
