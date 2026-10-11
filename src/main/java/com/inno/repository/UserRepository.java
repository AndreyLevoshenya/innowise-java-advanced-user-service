package com.inno.repository;

import com.inno.repository.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Modifying
    @Query(value = """
        UPDATE users
        SET active = :active
        WHERE id = :id
        """, nativeQuery = true)
    int updateActiveStatus(
            @Param("id") Long id,
            @Param("active") boolean active
    );
}
