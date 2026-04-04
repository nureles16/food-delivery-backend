package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {


    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    List<RefreshToken> findAllByUserIdAndRevokedFalseOrderByCreatedAtAsc(UUID userId);

}