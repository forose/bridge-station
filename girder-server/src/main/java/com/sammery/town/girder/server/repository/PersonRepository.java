package com.sammery.town.girder.server.repository;

import com.sammery.town.girder.server.model.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, Integer> {

    /**
     * 通过md5找人
     * @param md5 加密信息
     * @return 人实体
     */
    PersonEntity getFirstByMd5Equals(String md5);
}
