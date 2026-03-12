-- 글쓰기 양식 샘플 데이터 (섹션 기반)
-- 실행 전 board_template_sections, board_templates 테이블이 생성되어 있어야 합니다 (앱 1회 기동 후 실행)

-- ── 섹션 생성 ──
INSERT INTO board_template_sections (NAME, SORT_ORDER, CREATED_AT) VALUES
('업무',   1, NOW()),
('경조사', 2, NOW()),
('기타',   3, NOW());

-- ── 양식 생성 (SECTION_ID: 업무=1, 경조사=2, 기타=3) ──
-- 실제 ID는 auto_increment 값에 따라 달라질 수 있으므로,
-- 아래 서브쿼리로 섹션명을 조회하여 FK를 채웁니다.

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '주간 업무 보고', '<h2>주간 업무 보고</h2><p><strong>보고자:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>보고일:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>부서:</strong>&nbsp;</p><hr><h3>이번 주 완료 업무</h3><ul><li>&nbsp;</li><li>&nbsp;</li><li>&nbsp;</li></ul><h3>다음 주 예정 업무</h3><ul><li>&nbsp;</li><li>&nbsp;</li><li>&nbsp;</li></ul><h3>이슈 / 건의사항</h3><p>&nbsp;</p>',
    id, 1, 1, NOW(), NOW() FROM board_template_sections WHERE NAME = '업무' LIMIT 1;

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '장애·이슈 보고', '<h2>장애 · 이슈 보고</h2><p><strong>발생 일시:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>보고자:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>영향 범위:</strong>&nbsp;</p><hr><h3>현상</h3><p>&nbsp;</p><h3>원인</h3><p>&nbsp;</p><h3>조치 사항</h3><p>&nbsp;</p><h3>재발 방지 대책</h3><p>&nbsp;</p>',
    id, 1, 2, NOW(), NOW() FROM board_template_sections WHERE NAME = '업무' LIMIT 1;

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '업무 협조 요청', '<h2>업무 협조 요청</h2><p><strong>요청자:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>요청일:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>처리 기한:</strong>&nbsp;</p><p><strong>요청 부서:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>수신 부서:</strong>&nbsp;</p><hr><h3>요청 내용</h3><p>&nbsp;</p><h3>요청 사유</h3><p>&nbsp;</p><h3>참고 사항</h3><p>&nbsp;</p>',
    id, 1, 3, NOW(), NOW() FROM board_template_sections WHERE NAME = '업무' LIMIT 1;

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '경조금 신청', '<h2>경조금 신청</h2><p><strong>신청자:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>부서:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>신청일:</strong>&nbsp;</p><hr><p><strong>경조 구분:</strong>&nbsp;□ 결혼&nbsp;&nbsp;□ 출산&nbsp;&nbsp;□ 상(부)&nbsp;&nbsp;□ 상(모)&nbsp;&nbsp;□ 상(배우자)&nbsp;&nbsp;□ 기타(　　　　)</p><p><strong>경조 대상:</strong>&nbsp;</p><p><strong>경조 일시:</strong>&nbsp;</p><p><strong>경조 장소:</strong>&nbsp;</p><p><strong>입금 계좌:</strong>&nbsp;&nbsp;은행명:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;계좌번호:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;예금주:&nbsp;</p><h3>비고</h3><p>&nbsp;</p>',
    id, 1, 1, NOW(), NOW() FROM board_template_sections WHERE NAME = '경조사' LIMIT 1;

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '경조사 안내', '<h2>경조사 안내</h2><p>아래와 같이 경조사가 발생하여 안내드립니다.</p><hr><p><strong>구분:</strong>&nbsp;</p><p><strong>대상:</strong>&nbsp;</p><p><strong>일시:</strong>&nbsp;</p><p><strong>장소:</strong>&nbsp;</p><p><strong>연락처:</strong>&nbsp;</p><h3>비고</h3><p>&nbsp;</p>',
    id, 1, 2, NOW(), NOW() FROM board_template_sections WHERE NAME = '경조사' LIMIT 1;

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '회의록', '<h2>회의록</h2><p><strong>일시:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>장소:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>작성자:</strong>&nbsp;</p><p><strong>참석자:</strong>&nbsp;</p><hr><h3>안건</h3><ol><li>&nbsp;</li><li>&nbsp;</li></ol><h3>논의 내용</h3><p>&nbsp;</p><h3>결정 사항</h3><ul><li>&nbsp;</li><li>&nbsp;</li></ul><h3>향후 계획</h3><ul><li>&nbsp;</li><li>&nbsp;</li></ul>',
    id, 1, 1, NOW(), NOW() FROM board_template_sections WHERE NAME = '기타' LIMIT 1;

INSERT INTO board_templates (NAME, CONTENT, SECTION_ID, IS_ACTIVE, SORT_ORDER, CREATED_AT, UPDATED_AT)
SELECT '공지사항', '<h2>공지사항</h2><p><strong>공지일:</strong>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strong>공지 부서:</strong>&nbsp;</p><hr><h3>내용</h3><p>&nbsp;</p><h3>문의</h3><p>&nbsp;</p>',
    id, 1, 2, NOW(), NOW() FROM board_template_sections WHERE NAME = '기타' LIMIT 1;
