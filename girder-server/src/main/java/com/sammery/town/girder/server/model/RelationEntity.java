package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "relation")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter
public class RelationEntity extends BaseEntity{
    @Column
    private Integer person;
    @Column
    private Integer service;
}
