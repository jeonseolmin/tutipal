package com.tutipal.domain.qna.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.tutipal.domain.qna.entity.Comment;
import com.tutipal.domain.qna.entity.Post;
import com.tutipal.domain.qna.repository.CommentRepository;
import com.tutipal.domain.qna.repository.PostRepository;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

class QnaServiceTest {

    static final long TEACHER = 1L, AUTHOR = 2L, OTHER = 3L, SID = 10L;

    PostRepository posts = mock(PostRepository.class);
    CommentRepository comments = mock(CommentRepository.class);
    StudySessionService sessions = mock(StudySessionService.class);
    UserService users = mock(UserService.class);
    QnaService service = new QnaService(posts, comments, sessions, users, mock(SessionEvents.class));
    Post post = new Post(SID, AUTHOR, "질문", "for 문이 헷갈려요");
    Comment comment = new Comment(5L, AUTHOR, "저도요");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(post, "id", 5L);
        ReflectionTestUtils.setField(comment, "id", 9L);
        when(posts.findById(5L)).thenReturn(Optional.of(post));
        when(comments.findById(9L)).thenReturn(Optional.of(comment));
        when(sessions.requireActiveRole(TEACHER, SID)).thenReturn(SessionRole.TEACHER);
        when(sessions.requireActiveRole(AUTHOR, SID)).thenReturn(SessionRole.STUDENT);
        when(sessions.requireActiveRole(OTHER, SID)).thenReturn(SessionRole.STUDENT);
        when(users.namesOf(any())).thenReturn(Map.of());
    }

    private static HttpStatus status(Runnable call) {
        try {
            call.run();
        } catch (ApiException e) {
            return e.getStatus();
        }
        throw new AssertionError("ApiException 이 나야 함");
    }

    @Test
    void otherStudentCannotDeleteButAuthorAndTeacherCan() {
        assertThat(status(() -> service.deletePost(OTHER, 5L))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(() -> service.deleteComment(OTHER, 9L))).isEqualTo(HttpStatus.FORBIDDEN);
        verify(posts, never()).delete(any());
        service.deleteComment(TEACHER, 9L);
        verify(comments).delete(comment);
        service.deletePost(AUTHOR, 5L);
        verify(posts).delete(post);
    }

    @Test
    void detailMarksMine() {
        assertThat(service.get(AUTHOR, 5L).mine()).isTrue();
        assertThat(service.get(OTHER, 5L).mine()).isFalse();
    }
}
