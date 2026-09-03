package ru.yandex.practicum.product.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.product.entity.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrue();

    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrueAndCategoryId(Long categoryId);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN FETCH p.category
    WHERE p.active = true
    AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
    ORDER BY p.id
    """)
    List<Product> searchByName(@Param("name") String name);
}
