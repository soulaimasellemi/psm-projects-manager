package com.smartsoft.psm.repository;

import com.smartsoft.psm.entity.Tache;
import com.smartsoft.psm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TacheRepository extends JpaRepository<Tache, Long> {

    /** Tâches assignées à un utilisateur (désassignées avant la suppression du compte) */
    List<Tache> findByAssigne(User assigne);
}
