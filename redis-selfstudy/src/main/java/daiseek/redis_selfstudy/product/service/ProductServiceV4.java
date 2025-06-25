package daiseek.redis_selfstudy.product.service;

import daiseek.redis_selfstudy.product.Product;
import daiseek.redis_selfstudy.product.repository.ProductRepository;
import daiseek.redis_selfstudy.product.repository.ProductRepositoryV1;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 * Spring Cache Abstraction - Cache-Aside 전략
 * @Cacheable 애노테이션만 활용
 */


@Service
@Qualifier("productServiceV4")
public class ProductServiceV4 implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceV4(@Qualifier("inMemoryProductRepository") ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     *
     * @param id : 조회하려는 상품의 아이디
     * @return
     */
    @Override
    @Cacheable(value = "product", key = "#id") // 메서드의 반환 값을 캐시에 저장 혹은 캐시에서 조회하도록 지시
    public Optional<Product> getProductById(Long id) {
        System.out.println("ProductServiceV4: 캐시 미스. DB에서 찾아오기: " + id);
        return productRepository.findById(id); // 캐시에 없을때 DB에서 찾아서 반환해줌
    }

    /**
     * 저장: 캐시 반영 없음 (Cache-Aside의 특징)
     */
    @Override
    @CacheEvict(value = "product", key = "#result.id")
    public Product saveProduct(Product product) {
        Product saved = productRepository.save(product);
        System.out.println("DB 저장 후 캐시 무효화 수행: " + saved.getId());
        return saved;
    }

    /**
     * 수정: 캐시 반영 없음 (Cache-Aside의 특징)
     */
    @Override
    @CacheEvict(value = "product", key = "#result.id")
    public Product updateProduct(Product product) {
        // 검증 로직 생략
        Product updated = productRepository.save(product);
        System.out.println("DB 업데이트 후 캐시 무효화 수행: " + updated.getId());
        return updated;
    }

    /**
     * 삭제: 캐시 반영 없음 (Cache-Aside의 특징)
     */
    @Override
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        System.out.println("DB 삭제 후 캐시 무효화 수행: " + id);
    }
}

/**
 * 캐시 애노테이션 설명
 * 1. @Cacheable - 캐시 히트/미스
 * - 조회 시 캐시가 있으면, 메서드를 실행하지 않고 캐싱되어 있는 데이터 반환 -> 읽기 작업에 최적화
 *
 * 2. @CachePut - 캐싱 데이터 갱신
 * - 메서드를 항상 실행하고, 반환 값을 캐시에 저장 -> 쓰기 작업 이후 캐시에 데이터 저장
 * - 주로 저장, 수정 로직에 사용
 *
 * 3. @CacheEvict - 캐시 무효화(갱신)
 * - 해당 캐싱 데이터를 무효화함
 * - 삭제, 갱신 시 사용
 */
