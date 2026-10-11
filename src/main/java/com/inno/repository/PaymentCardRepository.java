package com.inno.repository;

import com.inno.repository.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

    long countByUserId(Long userId);

    @Query("""
        SELECT c
        FROM PaymentCard c
        WHERE c.user.id = :userId
        """)
    List<PaymentCard> findAllByUserId(Long userId);

    @Modifying
    @Query("""
        UPDATE PaymentCard c
        SET c.active = :active
        WHERE c.id = :id
        """)
    int updateActiveStatus(
            @Param("id") Long id,
            @Param("active") boolean active
    );
}
