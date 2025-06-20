package com.example.datarouter.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Data;

@Entity
@Data
public class RouteConfig {
    @Id
    private String routeId;

    @Column(nullable = false)
    private String headerName;

    @Column(nullable = false)
    private String headerValue;

    @Column(nullable = false)
    private String destinationChannel;

    private String description;
}
