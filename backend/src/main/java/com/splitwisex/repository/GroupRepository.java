package com.splitwisex.repository;

import com.splitwisex.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * Groups the given user belongs to (as any member, not just the
     * creator), newest first. Backs GET /api/groups.
     */
    @Query("SELECT gm.group FROM GroupMember gm WHERE gm.user.id = :userId ORDER BY gm.group.createdAt DESC")
    List<Group> findAllForUser(@Param("userId") Long userId);
}
