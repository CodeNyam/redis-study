package daiseek.redis_selfstudy.product.repository;

import daiseek.redis_selfstudy.product.Product;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Qualifier("inMemoryProductRepository")
public class ProductRepositoryV1 implements ProductRepository{

    // 실제 DB 역할을 대신할 Map (스레드 안전성을 위해 ConcurrentHashMap을 사용할 수도 있음)
    private final Map<Long, Product> storage = new HashMap<>();
    private final AtomicLong sequence = new AtomicLong(0); // ID 생성을 위한 시퀀스

    // 새로운 상품 저장
    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(sequence.incrementAndGet()); // 새 ID 할당
        }
        storage.put(product.getId(), product);
        System.out.println("메모리에 Product 저장: " + product);
        return product;
    }

    // ID로 상품 조회
    public Optional<Product> findById(Long id) {
        System.out.println("메모리에서 Product 조회: " + id);
        // 실제 DB 조회 지연을 흉내내기 위해 잠시 대기
        try {
            Thread.sleep(500); // 0.5초 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.ofNullable(storage.get(id));
    }

    // 상품 삭제
    public void deleteById(Long id) {
        System.out.println("메모리에서 Product 삭제: " + id);
        storage.remove(id);
    }
}