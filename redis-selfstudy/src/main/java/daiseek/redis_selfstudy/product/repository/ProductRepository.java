package daiseek.redis_selfstudy.product.repository;

import daiseek.redis_selfstudy.product.Product;

import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    void deleteById(Long id);
}
