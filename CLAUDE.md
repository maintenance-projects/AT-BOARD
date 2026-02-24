# AT-BOARD Project

## Project Overview
- Spring Boot 2.x, Java 8, MariaDB, Thymeleaf, Lombok, JPA
- Dual datasource: `atboard` (primary) + `msgadm` (secondary)
- Port: 8090
- Build: `./gradlew build -x test`
- Git user: hyunjunny / hjkim@ultari.co.kr

## Key Architecture
- Primary DB: board_categories, boards, admins, comments, board_likes
- Secondary DB: MSG_USER (users), MSG_PART (departments)
- Custom naming strategy: `CustomPhysicalNamingStrategy` + `DatabaseTableConfig`
- Multi-dept support: `board_category_depts` join table (ElementCollection)

## Important Patterns
- Java 8: no switch expressions, no `String.isBlank()` (use `trim().isEmpty()`)
- Transactions: `@Transactional("primaryTransactionManager")` or `"secondaryTransactionManager"`
- Admin session: `session.getAttribute("adminUser")` → `Admin`
- User session: `session.getAttribute("currentUser")` → `User`
- open-in-view=false → 트랜잭션 밖에서 LAZY 접근 불가 (LazyInitializationException 주의)

## SSO 진입점
- Header 기반: `X-User-Id` 헤더 → `SsoAuthInterceptor` (인터셉터)
- URL 기반: `/{userId}` → `SsoEntryController` → 세션 설정 후 `redirect:/board`

## User 객체 (session)
- `User.userId`, `User.userName`, `User.posName`, `User.deptId`
- `User.deptName`: `@Transient` (DB 미저장, 로그인 시 DepartmentService.getDeptName()으로 설정)

## Board / Comment 작성자 표시
- `Board.authorDeptName`, `Board.authorName`, `Board.authorPosName` 컬럼 모두 저장
- `Board.getAuthorName()`: authorDeptName + authorName + authorPosName 조합 반환 (Lombok getter 오버라이드)
- `Board.getAuthorNameOnly()`: authorName 필드만 반환
- `Comment.getAuthorDisplayName()`: 동일 패턴
- 작성 시 author.getDeptName() 우선, null이면 departmentService.getDeptName() 폴백

## 성능 최적화
- HikariCP: primary 20, secondary 10 (application.properties)
- Hibernate: default_batch_fetch_size=100, jdbc.batch_size=50, order_inserts/updates=true
- BoardCategory.deptIds: @BatchSize(100)
- Comment.board: FetchType.LAZY
- 조회수 원자적 UPDATE: boardRepository.incrementViewCount(id)
- 카테고리 삭제: 벌크 DELETE 3개 (comment → boardLike → board 순서)
- show-sql=false

## Multi-Department Board
- `BoardCategory.deptIds`: `@ElementCollection Set<String>` in `board_category_depts`
- `DepartmentService.isInAnyDeptOrSubDept(userDeptId, Set<String> targetDeptIds)`: 부서 트리 탐색
- Access check: `!category.getDeptIds().isEmpty() && !isInAnyDeptOrSubDept(...)`

## Key Files
- `BoardCategory.java`: primary model with deptIds ElementCollection
- `DepartmentService.java`: isInAnyDeptOrSubDept, getSelfAndAncestorDeptIds, getDeptName
- `BoardCategoryService.java`: getDeptCategoriesForUser
- `BoardService.java`: access checks, createBoard, getBoardById (원자적 조회수)
- `CommentService.java`: createComment, getCommentsByBoard (@Transactional readOnly)
- `Dept.java`: secondary entity (MSG_PART, fields: deptId, deptName, parentDept)
- `SsoEntryController.java`: GET /{userId} 진입점

## CSS
- `board.css`: .board-sidebar__avatar (global), .board-sidebar__user (PC @media min-width:1024px), .category-menu__user (mobile)
- `app.css`: `input, textarea, select { font-size: 16px !important; }` — iOS Safari 자동 확대 방지

## 주의사항 (버그 예방)
- `@Modifying`에 `clearAutomatically=true` 사용 금지: 영속성 컨텍스트 초기화 시 EAGER ElementCollection(deptIds)도 detach되어 LazyInitializationException 발생
- Java 8 호환: switch expression, text block, var 사용 불가
