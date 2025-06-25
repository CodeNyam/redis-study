package daiseek.redis_selfstudy.product.service;

import daiseek.redis_selfstudy.product.Product;
import daiseek.redis_selfstudy.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Write-Through 전략 사용
 * 데이터를 변경/쓰기할 때 캐시와 데이터베이스(메모리)에서 동시에 반영된다.
 * 앱이 캐시에, 캐시가 데이터베이스(메모리)에 데이터를 쓰도록 한다.
 */

/** 장점
 * 1. 데이터 일관성: 캐시와 데이터베이스의 데이터가 항상 동기화되어있음.
 * 2. 데이터가 항상 캐시에 있어서 읽을때 항상 캐시 히트를 고려할 수 있음.
 */

/** 단점
 * 1. 쓰기 성능 저하: 데이터 쓰기 연산시 캐시와 데이터베이스에 모두 써야함.
 * 2. 불필요한 캐싱 : 자주 조회되지 않는 데이터도 쓰기 연산시 모두 캐싱되므로 캐시 메모리를 잡아먹음.
 * 3. 캐시 방출 정책을 세우기 복잡함 - 캐싱 데이터가 제거될 때 DB에 동일한 데이터가 남아있는지 확인하는 로직이 필요함.
 */

@Service
@Qualifier("productServiceV2")
public class ProductServiceV2 implements ProductService{

    private final ProductRepository productRepositoryV1; // 가상 DB 역할
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long CACHE_TTL_SECONDS = 300; // 5분

    public ProductServiceV2(@Qualifier("inMemoryProductRepository") ProductRepository productRepositoryV1,
                            RedisTemplate<String, Object> redisTemplate) {
        this.productRepositoryV1 = productRepositoryV1;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 캐싱된 데이터 조회 - Cache-Aside와 동일함
     * @param id : 조회하려는 상품의 아이디
     * @return
     */
    @Override
    public Optional<Product> getProductById(Long id) {
        String cacheKey = "product:" + id;
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);

        if (cachedProduct != null) {
            System.out.println("ProductServiceV2: Cache hit for product ID: " + id);
            return Optional.of(cachedProduct);
        }

        System.out.println("ProductServiceV2: Cache miss for product ID: " + id);
        Optional<Product> productOptional = productRepositoryV1.findById(id);

        productOptional.ifPresent(product -> {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            System.out.println("ProductServiceV2: Caching product: " + product);
        });

        System.out.println("ProductServiceV2: Fetching product from DB: " + id);
        return productOptional;
    }

    /** 상품 생성/업데이트 (Write-Through)
     * DB에 저장/업데이트 후 캐시에도 바로 반영합니다.
     */
    @Override
    public Product saveProduct(Product product) {
        // 1. DB에 저장 (생성 또는 업데이트)
        Product savedProduct = productRepositoryV1.save(product);
        System.out.println("ProductServiceV2: Saving/Updating product in DB: " + savedProduct);

        // 2. 캐시에 바로 반영 (Write-Through의 핵심)
        String cacheKey = "product:" + savedProduct.getId();
        redisTemplate.opsForValue().set(cacheKey, savedProduct, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        System.out.println("ProductServiceV2: Write-Through: Caching product immediately: " + cacheKey);

        return savedProduct;
    }

    /** 상품 수정 (Write-Through)
     * 기존 Product를 업데이트하고 캐시에도 바로 반영합니다.
     */
    @Override
    public Product updateProduct(Product product) {
        // ID 유효성 검사 (Cache-Aside와 동일)
        if (product.getId() == null || !productRepositoryV1.findById(product.getId()).isPresent()) {
            throw new IllegalArgumentException("상품의 아이디 " + product.getId() + " - 해당 아이디의 상품을 찾지 못하였습니다.");
        }

        // 1. DB에 업데이트
        Product updatedProduct = productRepositoryV1.save(product);
        System.out.println("ProductServiceV2: Updating product in DB: " + updatedProduct);

        // 2. 캐시에 바로 반영 (Write-Through의 핵심)
        String cacheKey = "product:" + updatedProduct.getId();
        redisTemplate.opsForValue().set(cacheKey, updatedProduct, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        System.out.println("ProductServiceV2: Write-Through: Caching product immediately after update: " + cacheKey);

        return updatedProduct;
    }


    /** 상품 삭제 (Write-Through)
     * DB에서 삭제 후 캐시에서도 삭제합니다.
     */
    @Override
    public void deleteProduct(Long id) {
        // 1. DB에서 삭제
        productRepositoryV1.deleteById(id);
        System.out.println("ProductServiceV2: Deleting product from DB with ID: " + id);

        // 2. 캐시에서 삭제
        String cacheKey = "product:" + id;
        redisTemplate.delete(cacheKey);
        System.out.println("ProductServiceV2: Deleting from cache: " + cacheKey);
    }
}
