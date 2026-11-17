package com.tutipal.domain.material.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.material.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    /** 목록용: 파일 내용(data)은 읽지 않는다 */
    record MaterialRow(Long id, Long sessionId, String filename, int sizeBytes, Material.Status status,
                       int chunkCount, String error, Instant createdAt) {
    }

    @Query("select new com.tutipal.domain.material.repository.MaterialRepository$MaterialRow("
            + "m.id, m.sessionId, m.filename, m.sizeBytes, m.status, m.chunkCount, m.error, m.createdAt) "
            + "from Material m where m.sessionId = :sessionId order by m.id desc")
    List<MaterialRow> rows(@Param("sessionId") Long sessionId);

    @Query("select new com.tutipal.domain.material.repository.MaterialRepository$MaterialRow("
            + "m.id, m.sessionId, m.filename, m.sizeBytes, m.status, m.chunkCount, m.error, m.createdAt) "
            + "from Material m where m.id = :id")
    java.util.Optional<MaterialRow> row(@Param("id") Long id);

    /** 처리 스레드(트랜잭션 밖)에서 부르므로 자체 트랜잭션 */
    @Transactional
    @Modifying
    @Query("update Material m set m.status = :status, m.chunkCount = :chunks, m.error = :error where m.id = :id")
    void finish(@Param("id") Long id, @Param("status") Material.Status status, @Param("chunks") int chunks,
                @Param("error") String error);
}
