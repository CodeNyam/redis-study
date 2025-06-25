package daiseek.redis_selfstudy.product.service;

import daiseek.redis_selfstudy.product.Product;
import daiseek.redis_selfstudy.product.repository.ProductRepositoryV1;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Note. Cache-Aside 전략 사용
 * 앱 레벨에서 캐시를 관리하는 가장 기본적인 전략
 * 주로 읽기 연산에 최적화 되어있다.
 */

/** 장점
 * 1. 구현하기 쉽다.
 * 2. 캐시 키, 데이터 유효 시간(TTL), 캐시 무효화 시점 등 세밀하게 제어할 수 있어 유연하다.
 */

/** 단점
 * 1. 캐시에 없는 데이터일때 DB에서 데이터 조회 후 redis에도 해당 데이터를 저장해야하므로 시간이 더 든다.
 * 2. 데이터 변경이 잦으면 캐시 내 데이터의 신선도가 떨어진다.
 * 특히, 서버가 분산되어있다면, 캐싱 데이터의 신뢰도가 중요하므로 캐시 무효화 전략을 잘 짜야한다.
 */

/** 사용하기 좋은 상황
 *  1. 데이터 변경이 잦지 않을때
 *  2. 읽기 연산이 대부분일때
 */

@Service
@Qualifier("productServiceV1")
public class ProductServiceV1 implements ProductService {

    private final ProductRepositoryV1 productRepositoryV1;
    private final RedisTemplate<String, Object> redisTemplate; // RedisTemplate 주입

    // 생성자 주입
    public ProductServiceV1(@Qualifier("inMemoryProductRepository") ProductRepositoryV1 productRepositoryV1, RedisTemplate<String, Object> redisTemplate) {
        this.productRepositoryV1 = productRepositoryV1;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Cache-Aside 패턴으로 Product를 조회
     * 과정
     * 1. 캐시(Redis)에서 조회
     * 2. 캐시에 없으면, Cache Miss 발생! DB(메모리)에서 조회
     * 3. DB(메모리)에서 조회한 데이터를 캐시에 저장
     * 4. 데이터 반환
     */
    public Optional<Product> getProductById(Long id) {
        // 1. 캐시(Redis)에서 데이터 조회
        String cacheKey = "product:" + id; // 캐시 키 정의 (예: "product:1")
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);

        if (cachedProduct != null) {
            System.out.println("캐시(Redis)에서 Product 조회: " + id + " -> " + cachedProduct);
            return Optional.of(cachedProduct); // 캐시에 있으면 바로 반환
        }

        // 2. 캐시에 없으면 DB(가상)에서 조회 (Cache Miss)
        System.out.println("캐시 미스 발생. 데이터베이스(가상)에서 Product 조회: " + id);
        Optional<Product> productFromDb = productRepositoryV1.findById(id);

        // 3. DB에서 조회한 데이터가 있으면 캐시에 저장
        productFromDb.ifPresent(product -> {
            redisTemplate.opsForValue().set(cacheKey, product, 5, TimeUnit.MINUTES); // 5분 TTL (Time To Live) 설정
            System.out.println("데이터베이스에서 조회한 Product를 캐시(Redis)에 저장: " + product);
        });

        // 4. 데이터 반환
        return productFromDb;
    }

    /** 캐싱 무효화 - 데이터 생성
     * Product를 저장하고 이전에 캐싱된 데이터를 무효화합니다 (Create/Update).
     */
    public Product saveProduct(Product product) {
        Product savedProduct = productRepositoryV1.save(product); // DB(가상)에 저장

        // 캐시 무효화 (기존 캐시 삭제)
        String cacheKey = "product:" + savedProduct.getId();
        redisTemplate.delete(cacheKey);
        System.out.println("Product 저장/업데이트 후 캐시(Redis) 무효화: " + cacheKey);

        return savedProduct;
    }

    /** 캐싱 무효화 - 데이터 삭제
     * Product를 삭제하고 이전에 캐싱된 데이터를 무효화합니다 (Delete).
     */
    public void deleteProduct(Long id) {
        productRepositoryV1.deleteById(id); // DB(가상)에서 삭제

        // 캐시 무효화 (기존 캐시 삭제)
        String cacheKey = "product:" + id;
        redisTemplate.delete(cacheKey);
        System.out.println("Product 삭제 후 캐시(Redis) 무효화: " + cacheKey);
    }

    /**
     * 상품 수정 메서드
     * @param product : 수정하려는 상품 객체
     * @return : 수정된 상품 객체 데이터
     */
    @Override
    public Product updateProduct(Product product) {
        // ID가 있는 경우에만 업데이트 처리
        if (product.getId() == null || !productRepositoryV1.findById(product.getId()).isPresent()) {
            throw new IllegalArgumentException("상품의 아이디 " + product.getId() + " - 해당 아이디의 상품을 찾지 못하였습니다.");
        }

        Product updatedProduct = productRepositoryV1.save(product); // DB(가상)에 업데이트
        System.out.println("ProductServiceV1: 메모리에서 상품 업데이트 중...: " + updatedProduct);

        // 캐시 무효화 (기존 캐시 삭제)
        String cacheKey = "product:" + updatedProduct.getId();
        System.out.println("DEBUG: updateProduct에서 캐시 삭제 시도 키: " + cacheKey);
        redisTemplate.delete(cacheKey);
        System.out.println("상품 업데이트 후 캐시(Redis) 무효화: " + cacheKey);

        return updatedProduct;
    }
}
