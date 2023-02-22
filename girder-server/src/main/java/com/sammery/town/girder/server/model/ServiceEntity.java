package com.sammery.town.girder.server.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "service") @Getter @Setter @Table
public class ServiceEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String host;
    @Column
    private Integer port;
    @Column
    private String name;
}
