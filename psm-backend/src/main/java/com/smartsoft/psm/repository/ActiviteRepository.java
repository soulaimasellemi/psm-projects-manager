package com.smartsoft.psm.repository;

import com.smartsoft.psm.entity.Activite;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActiviteRepository extends JpaRepository<Activite, Long> {

    @Query("""
            select a from Activite a
            where (lower(coalesce(a.auteurNom, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.auteurEmail, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.details, '')) like lower(concat('%', :q, '%')))
              and (:cible is null or a.cible = :cible)
              and (:action is null or a.action = :action)
            """)
    Page<Activite> search(@Param("q") String q, @Param("cible") Cible cible, @Param("action") Action action,
                          Pageable pageable);
}
