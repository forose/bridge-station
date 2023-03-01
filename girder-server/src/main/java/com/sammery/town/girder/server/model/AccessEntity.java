package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "access")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter
public class AccessEntity extends BaseEntity{
    @Column
    private String person;
    @Column
    private String service;
}
