package com.smartsoft.psm.repository;

import com.smartsoft.psm.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    @Query("""
            select c from Client c
            where lower(c.nom) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.secteur, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.adresse, '')) like lower(concat('%', :q, '%'))
            """)
    Page<Client> search(@Param("q") String q, Pageable pageable);

    List<Client> findAllByOrderByNomAsc();
}
