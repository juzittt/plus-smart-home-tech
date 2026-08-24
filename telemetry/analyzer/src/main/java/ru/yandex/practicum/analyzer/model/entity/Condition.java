package ru.yandex.practicum.analyzer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.analyzer.model.enums.ConditionOperation;
import ru.yandex.practicum.analyzer.model.enums.ConditionType;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConditionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false)
    private ConditionOperation operation;

    @Column(name = "value")
    private Integer value;

    public Condition(ConditionType type, ConditionOperation operation, Integer value) {
        this.type = type;
        this.operation = operation;
        this.value = value;
    }
}
