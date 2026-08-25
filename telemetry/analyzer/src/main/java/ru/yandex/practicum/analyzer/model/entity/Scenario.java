package ru.yandex.practicum.analyzer.model.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hub_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "hub_id", nullable = false)
    private String hubId;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ScenarioCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ScenarioAction> actions = new ArrayList<>();

    public Scenario(String hubId, String name) {
        this.hubId = hubId;
        this.name = name;
    }

    public void addCondition(ScenarioCondition condition) {
        conditions.add(condition);
        condition.setScenario(this);
    }

    public void addAction(ScenarioAction action) {
        actions.add(action);
        action.setScenario(this);
    }

    public void removeCondition(ScenarioCondition condition) {
        conditions.remove(condition);
        condition.setScenario(null);
    }

    public void removeAction(ScenarioAction action) {
        actions.remove(action);
        action.setScenario(null);
    }
}
