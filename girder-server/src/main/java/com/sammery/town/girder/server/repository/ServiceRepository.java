package com.sammery.town.girder.server.repository;

import com.sammery.town.girder.server.model.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Integer> {
    /**
     * 通过id获取所有
     * @param ids id
     * @return 服务
     */
    List<ServiceEntity> getAllByIdIn(List<Integer> id);
}
