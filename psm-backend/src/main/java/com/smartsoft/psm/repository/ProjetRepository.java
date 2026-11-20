package com.smartsoft.psm.repository;

import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProjetRepository extends JpaRepository<Projet, Long> {

    /**
     * Recherche multi-critères. Paramètres facultatifs (null = pas de filtre) :
     * statut, client, membre affecté, et "en retard" (fin prévue < aujourd'hui et non terminé).
     */
    String SEARCH = """
            select p from Projet p
            where (lower(p.libelle) like lower(concat('%', :q, '%'))
                or lower(p.client.nom) like lower(concat('%', :q, '%')))
              and (:statut is null or p.statut = :statut)
              and (:clientId is null or p.client.id = :clientId)
              and (:membreId is null or exists (select 1 from Projet p2 join p2.membres m where p2 = p and m.id = :membreId))
              and (:enRetard is null or (p.dateFinPrevue < :today and p.statut <> com.smartsoft.psm.entity.StatutProjet.TERMINE))
            """;

    @Query(SEARCH)
    Page<Projet> search(@Param("q") String q,
                        @Param("statut") StatutProjet statut,
                        @Param("clientId") Long clientId,
                        @Param("membreId") Long membreId,
                        @Param("enRetard") Boolean enRetard,
                        @Param("today") LocalDate today,
                        Pageable pageable);

    /** Même recherche, sans pagination (export Excel / PDF) */
    @Query(SEARCH)
    List<Projet> searchAll(@Param("q") String q,
                           @Param("statut") StatutProjet statut,
                           @Param("clientId") Long clientId,
                           @Param("membreId") Long membreId,
                           @Param("enRetard") Boolean enRetard,
                           @Param("today") LocalDate today,
                           Sort sort);

    long countByStatut(StatutProjet statut);

    @Query("""
            select count(p) from Projet p
            where p.dateFinPrevue < :today and p.statut <> com.smartsoft.psm.entity.StatutProjet.TERMINE
            """)
    long countEnRetard(@Param("today") LocalDate today);

    @Query("select coalesce(avg(p.avancement), 0) from Projet p where p.statut = com.smartsoft.psm.entity.StatutProjet.EN_COURS")
    Double avancementMoyenEnCours();

    @Query("select coalesce(sum(p.budget), 0.0) from Projet p")
    Double sumBudget();

    @Query("""
            select c.nom, sum(p.budget) from Projet p join p.client c
            group by c.id, c.nom
            order by sum(p.budget) desc
            """)
    List<Object[]> topClientsParBudget(Pageable pageable);

    List<Projet> findTop5ByOrderByIdDesc();

    /** Projets non terminés (calcul de la santé sur le dashboard) */
    List<Projet> findByStatutNot(StatutProjet statut);

    /** Projets en cours, pour le graphique d'avancement du dashboard */
    List<Projet> findTop8ByStatutOrderByAvancementDesc(StatutProjet statut);

    /** Projets auxquels un utilisateur est affecté (nettoyage avant suppression du compte) */
    List<Projet> findByMembresContaining(User user);

    long countByMembresContaining(User user);
}
