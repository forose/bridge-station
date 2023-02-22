package com.sammery.town.girder.server.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "relation") @Getter @Setter
public class RelationEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer person;
    @Column
    private Integer service;
}
