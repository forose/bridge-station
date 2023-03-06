package com.sammery.town.girder.server.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity(name = "service")
@EqualsAndHashCode(callSuper = true)
@Getter @Setter @Accessors(chain = true)
public class ServiceEntity extends BaseEntity{
    @Column
    private String host;
    @Column
    private Integer port;
    @Column
    private String name;
}
