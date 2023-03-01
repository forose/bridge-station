package com.sammery.town.girder.server.repository;

import com.sammery.town.girder.server.model.AccessEntity;
import com.sammery.town.girder.server.model.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessRepository extends JpaRepository<AccessEntity, Integer> {

}
