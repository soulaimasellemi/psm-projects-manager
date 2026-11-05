package com.smartsoft.psm.repository;

import com.smartsoft.psm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByEnabledFalse();

    @Query("""
            select u from User u
            where (lower(u.nom) like lower(concat('%', :q, '%'))
                or lower(u.prenom) like lower(concat('%', :q, '%'))
                or lower(u.email) like lower(concat('%', :q, '%')))
              and (:enabled is null or u.enabled = :enabled)
            """)
    Page<User> search(@Param("q") String q, @Param("enabled") Boolean enabled, Pageable pageable);

    @Query("select u from User u join u.roles r where r.libelle = :libelle and u.enabled = true")
    List<User> findActiveByRole(@Param("libelle") String libelle);
}
