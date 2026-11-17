# ERD

- 논리 ERD: `ERD-logical.png` / `.pdf` — 한국어 엔터티 · 속성, 식별자와 관계(1:N, 사용자–방 N:M 은 ‘방 참가’로 풂)만
- 물리 ERD: `ERD-physical.png` / `.pdf`, 아래 Mermaid — 실제 테이블 · 컬럼 · 타입 · 키

실제 DB(Flyway V1~V10 적용 후)에서 뽑았다. Flyway · Spring Session 테이블은 뺐다. 다시 뽑을 때는 이 그림들을 새로 만든다 (DB 를 바꾸면 같이 갱신).
같은 사람이 같은 방에 두 번 들어갈 수 없다 (`session_participants` 의 session_id+user_id 유니크), 질문 하나에 답은 한 사람당 하나 (`quiz_answers` 의 quiz_id+user_id 유니크).

| 테이블 | 뜻 |
|---|---|
| `comments` | 댓글 |
| `help_requests` | 도움 요청 |
| `material_chunks` | 자료 조각 + 임베딩 (RAG) |
| `materials` | 수업 자료 (파일) |
| `notices` | 공지 |
| `posts` | 게시판 글 |
| `quiz_answers` | 질문 답변 · 채점 |
| `quizzes` | 이해도 질문 |
| `review_items` | 복습 기록 (화면 요약 + 임베딩) |
| `session_participants` | 방 참가자 (방장/참여자, 즐겨찾기) |
| `study_sessions` | 방 (단체 학습) |
| `users` | 사용자 (소셜 로그인) |

```mermaid
erDiagram
    posts ||--o{ comments : "post_id"
    users ||--o{ comments : "author_id"
    study_sessions ||--o{ help_requests : "session_id"
    users ||--o{ help_requests : "user_id"
    materials ||--o{ material_chunks : "material_id"
    study_sessions ||--o{ materials : "session_id"
    users ||--o{ materials : "uploader_id"
    study_sessions ||--o{ notices : "session_id"
    users ||--o{ notices : "author_id"
    study_sessions ||--o{ posts : "session_id"
    users ||--o{ posts : "author_id"
    quizzes ||--o{ quiz_answers : "quiz_id"
    users ||--o{ quiz_answers : "user_id"
    study_sessions ||--o{ quizzes : "session_id"
    users ||--o{ review_items : "user_id"
    study_sessions ||--o{ session_participants : "session_id"
    users ||--o{ session_participants : "user_id"
    users ||--o{ study_sessions : "owner_id"
    comments {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint post_id FK
        bigint author_id FK
        varchar(1000) body
    }
    help_requests {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint session_id FK
        bigint user_id FK
        varchar(300) message
        boolean share_screen
        varchar(20) status "PENDING / ACCEPTED / DECLINED / DONE"
    }
    material_chunks {
        bigint id PK
        bigint material_id FK
        bigint session_id "검색용 복사 (FK 없음)"
        int page "null 가능"
        text content
        vector embedding "vector(1024) bge-m3"
    }
    materials {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint session_id FK
        bigint uploader_id FK
        varchar(200) filename
        int size_bytes
        bytea data
        varchar(20) status "PROCESSING / DONE / FAILED"
        int chunk_count
        varchar(200) error "null 가능"
    }
    notices {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint session_id FK
        bigint author_id FK
        varchar(100) title
        varchar(3000) body
    }
    posts {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint session_id FK
        bigint author_id FK
        varchar(100) title
        varchar(3000) body
    }
    quiz_answers {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint quiz_id FK,UK
        bigint user_id FK,UK
        int choice_index "null 가능"
        varchar(2000) text "null 가능"
        boolean correct "null 가능"
        varchar(20) graded_by "AUTO / AI / TEACHER, null 가능"
        varchar(500) feedback "null 가능"
    }
    quizzes {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint session_id FK
        varchar(20) type "CHOICE / TEXT"
        varchar(1000) question
        text_array choices "null 가능"
        int answer_index "null 가능"
        varchar(2000) model_answer "null 가능"
        timestamptz closes_at
    }
    review_items {
        bigint id PK
        bigint user_id FK
        varchar(500) summary
        vector embedding "vector(1024) bge-m3"
        int seen_count
        int seen_seconds
        timestamptz first_seen
        timestamptz last_seen
        timestamptz suggested_at "복습 제안한 시각, null 가능"
    }
    session_participants {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        bigint session_id FK,UK
        bigint user_id FK,UK
        varchar(20) role "TEACHER / STUDENT"
        boolean pinned "홈 즐겨찾기"
    }
    study_sessions {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        varchar(50) title
        varchar(30) topic "과목 이름 (직접 입력 가능)"
        varchar(6) invite_code UK "초대 코드 6자리"
        bigint owner_id FK "방장"
        smallint difficulty "챗봇 난이도 1~5"
        boolean hide_answers
        boolean prefer_materials
        timestamptz ended_at "방 닫힌 시각, null 가능"
    }
    users {
        bigint id PK
        timestamptz created_at
        timestamptz updated_at
        varchar(255) email "null 가능"
        varchar(50) name
        varchar(20) provider UK "kakao / naver / google / dev"
        varchar(100) provider_id UK
        varchar(20) role "USER / ADMIN"
        boolean review_consent "복습 기록 동의"
    }
```
