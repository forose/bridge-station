package com.sammery.town.girder.server.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Getter@Setter@MappedSuperclass@Accessors(chain = true)
public class BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /**
     * 创建时间
     */
    @Column
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /**
     * 更新时间
     */
    @Column
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
}
