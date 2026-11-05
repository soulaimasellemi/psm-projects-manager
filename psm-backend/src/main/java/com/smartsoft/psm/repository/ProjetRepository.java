package com.smartsoft.psm.repository;

import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjetRepository extends JpaRepository<Projet, Long> {

    @Query("""
            select p from Projet p
            where (lower(p.libelle) like lower(concat('%', :q, '%'))
                or lower(p.client.nom) like lower(concat('%', :q, '%')))
              and (:statut is null or p.statut = :statut)
              and (:clientId is null or p.client.id = :clientId)
            """)
    Page<Projet> search(@Param("q") String q,
                        @Param("statut") StatutProjet statut,
                        @Param("clientId") Long clientId,
                        Pageable pageable);

    long countByStatut(StatutProjet statut);

    @Query("select coalesce(sum(p.budget), 0.0) from Projet p")
    Double sumBudget();

    @Query("""
            select c.nom, sum(p.budget) from Projet p join p.client c
            group by c.id, c.nom
            order by sum(p.budget) desc
            """)
    List<Object[]> topClientsParBudget(Pageable pageable);

    List<Projet> findTop5ByOrderByIdDesc();
}
