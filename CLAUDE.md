# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
- Spring Boot 2.7.x, Java 8, MariaDB, Thymeleaf, Lombok, JPA
- Dual datasource: `atboard` (primary) + `msgadm` (secondary)
- Port: 8090
- Git user: hyunjunny / hjkim@ultari.co.kr

## Commands
```bash
./gradlew build -x test          # 빌드 (테스트 제외)
./gradlew bootRun                # 로컬 실행
./gradlew bootRun --args='--spring.profiles.active=dev'   # dev 프로파일
./gradlew bootRun --args='--spring.profiles.active=prod'  # prod 프로파일
```

## Key Architecture

### Dual Datasource
- Primary DB (`atboard`): board_categories, boards, admins, comments, board_likes, app_settings
- Secondary DB (`msgadm`): MSG_USER (users), MSG_PART (departments)
- 각각 별도의 EntityManagerFactory + TransactionManager
- Cross-DB 조인 불가 → 서비스 계층에서 메모리 합산
- DDL: primary=`update`, secondary=`validate` (secondary 스키마 변경은 DB에서 직접)
- Custom naming: `CustomPhysicalNamingStrategy` + `DatabaseTableConfig` (`database.table.*` properties로 컬럼명 동적 매핑)

### Schedulers
- `DepartmentSyncScheduler`: 매시간(`0 0 * * * *`) 부서 카테고리 자동 생성/삭제 동기화
- `AttachmentRetentionScheduler`: 보존 기간 만료 첨부파일 자동 삭제

## Important Patterns

### Java 8 제약
- switch expression, text block, `var` 사용 불가
- `String.isBlank()` 사용 불가 → `trim().isEmpty()` 사용
- `Comparator.comparing(lambda)` → StackOverflowError 가능 → 명시적 `(a, b) -> {}` 람다 사용

### 트랜잭션
- `@Transactional("primaryTransactionManager")` 또는 `"secondaryTransactionManager"` 명시 필수
- `open-in-view=false` → 트랜잭션 밖에서 LAZY 접근 불가 (LazyInitializationException 주의)
- `@Modifying`에 `clearAutomatically=true` 사용 금지: EAGER ElementCollection(deptIds)도 detach되어 LazyInitializationException 발생

### 세션
- Admin: `session.getAttribute("adminUser")` → `Admin`
- User: `session.getAttribute("currentUser")` → `User`

## SSO 진입점
- Header 기반: `X-User-Id` 헤더 → `SsoAuthInterceptor`
- URL 기반: `GET /{userId}` → `SsoEntryController` → 세션 설정 후 `redirect:/board`

## User 객체
- `User.userId`, `User.userName`, `User.posName`, `User.deptId`
- `User.deptName`: `@Transient` (DB 미저장, 로그인 시 `DepartmentService.getDeptName()`으로 설정)
- `User.userOrder`: 정렬용 varchar 컬럼 (`database.table.user.user-order` 설정)

## Board / Comment 작성자 표시
- `Board.authorDeptName`, `Board.authorName`, `Board.authorPosName` 컬럼 모두 저장
- `Board.getAuthorName()`: 세 필드 조합 반환 (Lombok getter 오버라이드)
- `Board.getAuthorNameOnly()`: authorName 필드만 반환
- `Comment.getAuthorDisplayName()`: 동일 패턴
- 작성 시 `author.getDeptName()` 우선, null이면 `departmentService.getDeptName()` 폴백

## Multi-Department Board
- `BoardCategory.deptIds`: `@ElementCollection Set<String>` → `board_category_depts` 테이블, `@BatchSize(100)`
- `DepartmentService.isInAnyDeptOrSubDept(userDeptId, Set<String> targetDeptIds)`: 부서 트리 탐색
- Access check: `!category.getDeptIds().isEmpty() && !isInAnyDeptOrSubDept(...)`
- `Dept.deptOrder`: 정렬용 varchar (`database.table.dept.dept-order` 설정), null/빈값 → "999999" 처리

## Comment AJAX 구조
- `templates/fragments/comments.html`: 댓글 섹션 프래그먼트 (`th:fragment="commentsList"`)
- `GET /board/{boardId}/comments/fragment` → `fragments/comments :: commentsList` (조회수 증가 없음, `getBoardByIdNoCount` 사용)
- `detail.html`: 이벤트 델리게이션으로 폼 submit 처리, `fetch(..., { redirect: 'manual' })`로 302 추적 방지

## 성능 최적화
- HikariCP: primary 50, secondary 20 (application.properties)
- Hibernate: `default_batch_fetch_size=100`, `jdbc.batch_size=50`, `order_inserts/updates=true`
- 조회수 원자적 UPDATE: `boardRepository.incrementViewCount(id)`
- 카테고리 삭제: 벌크 DELETE 순서 — comment → boardLike → board
- `show-sql=false`

## NotificationService
- `@Async` 비동기 실행 → 게시글 저장에 영향 없음
- `notification.server.type`: none / fmc / msg
- `notification.msg.link-url-enabled=true` → 사용자별 SSO linkUrl 포함 개별 요청
- linkUrl 형식: `{base-url}/{userId}/board/{boardId}?categoryId={categoryId}`
- 작성자 본인은 수신 대상 제외

## ProfilePhotoControllerAdvice
- 모든 컨트롤러에 `profilePhotoUrlPattern`을 `@ModelAttribute`로 주입
- 템플릿에서 `{userId}` 치환 패턴 사용, 이미지 로드 실패 시 letter avatar 유지 (board.js `loadProfilePhotos`)

## Key Files
- `BoardCategory.java`: primary model, deptIds ElementCollection
- `DepartmentService.java`: isInAnyDeptOrSubDept, getSelfAndAncestorDeptIds, getDeptCompositeSortKeyMap, getDeptName
- `BoardCategoryService.java`: getDeptCategoriesForUser
- `BoardService.java`: access checks, createBoard, getBoardById (원자적 조회수), getBoardByIdNoCount
- `CommentService.java`: createComment, getCommentsByBoard (@Transactional readOnly)
- `Dept.java`: secondary entity (MSG_PART)
- `SsoEntryController.java`: GET /{userId} 진입점
- `NotificationService.java`: @Async 알림 발송
- `DepartmentSyncScheduler.java`: 부서 카테고리 자동 동기화

## CSS
- `board.css`: 게시판 UI 전체, 모바일 반응형, Quill 툴바 sticky (`top: 70px`)
- `app.css`: 전역 스타일, `input/textarea/select { font-size: 16px !important }` (iOS 자동확대 방지), `html { scroll-padding-top: 78px }`
- 모바일 기본값 줄이고 `@media (min-width: 768px)` 이상에서 복원하는 패턴

## 파일 업로드
- 최대 파일 크기: 10MB, 요청 전체: 150MB
- 업로드 디렉토리: `uploads/` (application.properties `file.upload-dir`)

## 주의사항
- `@Modifying clearAutomatically=true` 사용 금지 (위 트랜잭션 섹션 참고)
- Java 8 호환 문법만 사용
- `replace_all=true` Edit 사용 시 같은 메서드를 호출하는 신규 메서드가 있으면 무한재귀 위험
- Cross-DB 정렬: `departmentRepository.findAll()` 후 인메모리 맵으로 처리 (composite key: `"%05d_%s", depth, deptOrder`)
