package com.tutipal.domain.qna.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.qna.dto.QnaDtos.CommentResponse;
import com.tutipal.domain.qna.dto.QnaDtos.CreateComment;
import com.tutipal.domain.qna.dto.QnaDtos.CreatePost;
import com.tutipal.domain.qna.dto.QnaDtos.PostDetail;
import com.tutipal.domain.qna.dto.QnaDtos.PostSummary;
import com.tutipal.domain.qna.entity.Comment;
import com.tutipal.domain.qna.entity.Post;
import com.tutipal.domain.qna.repository.CommentRepository;
import com.tutipal.domain.qna.repository.PostRepository;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

/** 게시판: 세션 참가자는 누구나 글 · 댓글. 지우기는 쓴 사람 또는 그 세션 선생님. */
@Service
@Transactional(readOnly = true)
public class QnaService {

    private final PostRepository posts;
    private final CommentRepository comments;
    private final StudySessionService sessions;
    private final UserService users;
    private final SessionEvents events;

    public QnaService(PostRepository posts, CommentRepository comments, StudySessionService sessions,
                      UserService users, SessionEvents events) {
        this.posts = posts;
        this.comments = comments;
        this.sessions = sessions;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public PostSummary createPost(Long userId, Long sessionId, CreatePost req) {
        sessions.requireActiveRole(userId, sessionId);
        Post p = posts.save(new Post(sessionId, userId, req.title().strip(), req.body().strip()));
        events.toSession(sessionId, "posts", p.getId());
        return summaries(userId, List.of(p)).getFirst();
    }

    public List<PostSummary> list(Long userId, Long sessionId) {
        sessions.requireActiveRole(userId, sessionId);
        return summaries(userId, posts.findBySessionIdOrderByIdDesc(sessionId));
    }

    public PostDetail get(Long userId, Long postId) {
        Post p = findPost(postId);
        sessions.requireActiveRole(userId, p.getSessionId());
        List<Comment> cs = comments.findByPostIdOrderByIdAsc(postId);
        Map<Long, String> names = users.namesOf(Stream.concat(Stream.of(p.getAuthorId()), cs.stream().map(Comment::getAuthorId)).toList());
        return new PostDetail(p.getId(), p.getTitle(), p.getBody(), name(names, p.getAuthorId()), p.getCreatedAt(),
                p.getAuthorId().equals(userId),
                cs.stream().map(c -> new CommentResponse(c.getId(), c.getBody(), name(names, c.getAuthorId()), c.getCreatedAt(),
                        c.getAuthorId().equals(userId))).toList());
    }

    @Transactional
    public PostDetail comment(Long userId, Long postId, CreateComment req) {
        Post p = findPost(postId);
        sessions.requireActiveRole(userId, p.getSessionId());
        comments.save(new Comment(postId, userId, req.body().strip()));
        events.toSession(p.getSessionId(), "posts", postId);
        return get(userId, postId);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post p = findPost(postId);
        requireAuthorOrTeacher(userId, p.getAuthorId(), p.getSessionId());
        posts.delete(p);   // 댓글은 ON DELETE CASCADE
        events.toSession(p.getSessionId(), "posts", postId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment c = comments.findById(commentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없어요"));
        Post p = findPost(c.getPostId());
        requireAuthorOrTeacher(userId, c.getAuthorId(), p.getSessionId());
        comments.delete(c);
        events.toSession(p.getSessionId(), "posts", p.getId());
    }

    private void requireAuthorOrTeacher(Long userId, Long authorId, Long sessionId) {
        SessionRole role = sessions.requireActiveRole(userId, sessionId);
        if (!authorId.equals(userId) && role != SessionRole.TEACHER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "쓴 사람이나 선생님만 지울 수 있어요");
        }
    }

    private List<PostSummary> summaries(Long userId, List<Post> list) {
        Map<Long, Long> counts = comments.findByPostIdIn(list.stream().map(Post::getId).toList()).stream()
                .collect(Collectors.groupingBy(Comment::getPostId, Collectors.counting()));
        Map<Long, String> names = users.namesOf(list.stream().map(Post::getAuthorId).toList());
        return list.stream()
                .map(p -> new PostSummary(p.getId(), p.getTitle(), name(names, p.getAuthorId()), counts.getOrDefault(p.getId(), 0L),
                        p.getCreatedAt(), p.getAuthorId().equals(userId)))
                .toList();
    }

    private static String name(Map<Long, String> names, Long id) {
        return names.getOrDefault(id, "(알 수 없음)");
    }

    private Post findPost(Long id) {
        return posts.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "글을 찾을 수 없어요"));
    }
}
