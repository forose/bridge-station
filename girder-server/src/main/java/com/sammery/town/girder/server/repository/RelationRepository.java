package com.sammery.town.girder.server.repository;

import com.sammery.town.girder.server.model.RelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelationRepository extends JpaRepository<RelationEntity, Integer> {
    /**
     * 通过人获取所有关系
     * @param person 人id
     * @return 关系
     */
    List<RelationEntity> getAllByPerson(Integer person);
}
