package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "access")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter @Accessors(chain = true)
public class AccessEntity extends BaseEntity{
    @Column
    private Integer person;
    @Column
    private String remote;
    @Column
    private String service;
}
