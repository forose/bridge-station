package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "service")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter
public class ServiceEntity extends BaseEntity{
    @Column
    private String host;
    @Column
    private Integer port;
    @Column
    private String name;
}
