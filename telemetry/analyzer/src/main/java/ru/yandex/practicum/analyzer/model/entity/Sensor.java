package ru.yandex.practicum.analyzer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sensors")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sensor {

    @Id
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private String id;

    @Column(name = "hub_id", nullable = false)
    private String hubId;

    public Sensor(String id, String hubId) {
        this.id = id;
        this.hubId = hubId;
    }
}
