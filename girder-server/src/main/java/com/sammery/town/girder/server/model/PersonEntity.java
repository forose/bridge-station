package com.sammery.town.girder.server.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "person") @Getter @Setter
public class PersonEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String md5;
    @Column
    private String identity;
}
