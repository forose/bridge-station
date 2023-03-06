package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity(name = "relation")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter @Accessors(chain = true)
public class RelationEntity extends BaseEntity{
    @Column
    private Integer person;
    @Column
    private Integer service;
}
