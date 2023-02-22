package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "person")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter
public class PersonEntity extends BaseEntity{
    @Column
    private String name;
    @Column(unique = true, nullable = false)
    private String md5;
    @Column
    private String identity;
}
