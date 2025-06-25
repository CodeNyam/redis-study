package daiseek.redis_selfstudy.product.service;

import daiseek.redis_selfstudy.product.Product;
import daiseek.redis_selfstudy.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Write-Back 전략
 * 세 전략 중 가장 복잡하지만, 쓰기 성능이 제일 좋다.
 * 데이터 변경 시 앱은 캐시에만 변경된 데이터를 반영하고, 즉시 응답한다.
 * 이때 캐시는 변경 데이터를 내부적으로 DirtyChecking 상태로 표시한다.
 * 이후 비동기적으로 실제 DB에 반영한다.
 */

/**
 * 단점
 * 1. 데이터 유실: 캐시에 데이터를 먼저 쓰기 때문에 쓰기 작업의 응답 속도가 빠르다.
 * 하지만 DB에 반영되지 않은 데이터가 있을때 캐시 서버가 다운되면 해당 데이터를 잃어버릴 수도 있다.
 */

/** Note. 순수한 자바 환경에서 Write-Back을 완벽하게 구현하긴 힘들다.
 * 1. 더티 체킹 기능을 구현하려면 추가적인 큐, 더티 플래그 등이 필요하다.
 * 2. 비동기 동기화 : 더티 데이터를 주기적으로 DB에 쓰는 스레드나 스케줄러를 구현해야 한다.
 * 3. 데이터 유실 방지 로직 : 캐시 서버 다운시 더티 데이터를 복구하는 메커니즘을 구현해야 한다.
 *
 * 따라서 이 파일에서는 Write-Back 전략의 아이디어만 보여주겠다.
 * 1. 쓰기 작업시 캐시에만 적용함. 즉시 반환되는 속도에 집중
 * 2. 비동기 동기화는 Runnable, ExecutorService를 이용하여 비슷하게 구현한다.
 */

/**
 * Note. Runnable, ExecutorService
 * 1. Runnable 인터페이스
 * 실행 가능한 코드 덩어리(task)를 나타내는 자바의 함수형 인터페이스
 * run() 메서드 하나만 갖는다.
 * 이 메서드 안에 각 스레드에서 실행하고 싶은 로직을 작성한다.
 * ExecutorService에서 실행할 하나의 작업을 정의할 때 Runnable을 상속받는 구현체를 사용한다.
 *
 *
 * 2. ExecutorService
 * 스레드 풀을 관리하고 Runnable 작업을 실행하는 프레임워크
 * 직접 스레드 생성 및 관리해준다.
 *
 */


@Service
@Qualifier("productServiceV3")
public class ProductServiceV3 implements ProductService{

    private final ProductRepository productRepositoryV1; // 가상 DB 역할
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long CACHE_TTL_SECONDS = 300; // 5분

    // Write-Back을 위한 비동기 처리용 ExecutorService
    private ExecutorService writeBackExecutor;

    public ProductServiceV3(@Qualifier("inMemoryProductRepository") ProductRepository productRepositoryV1,
                            RedisTemplate<String, Object> redisTemplate) {
        this.productRepositoryV1 = productRepositoryV1;
        this.redisTemplate = redisTemplate;
    }


    @PostConstruct
    public void init() {
        // 단일 스레드 Executor를 사용하여 순차적인 DB 업데이트를 시뮬레이션
        // 실제 운영에서는 스레드 풀 크기, 큐 관리, 에러 처리 등 복잡한 로직 필요
        writeBackExecutor = Executors.newSingleThreadExecutor();
        System.out.println("ProductServiceV3: Write-Back ExecutorService 초기화 완료.");
    }

    @PreDestroy
    public void shutdown() {
        // 애플리케이션 종료 시 ExecutorService 종료 및 남은 작업 처리
        writeBackExecutor.shutdown();
        try {
            if (!writeBackExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                writeBackExecutor.shutdownNow();
                System.err.println("ProductServiceV3: Write-Back ExecutorService 즉시 종료됨 (남은 작업 유실 가능성).");
            }
        } catch (InterruptedException e) {
            writeBackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            System.err.println("ProductServiceV3: Write-Back ExecutorService 종료 중 인터럽트 발생.");
        }
        System.out.println("ProductServiceV3: Write-Back ExecutorService 종료 완료.");
    }


    @Override
    public Optional<Product> getProductById(Long id) {
        String cacheKey = "product:" + id;
        Product cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);

        if (cachedProduct != null) {
            System.out.println("ProductServiceV3: Cache hit for product ID: " + id);
            return Optional.of(cachedProduct);
        }

        System.out.println("ProductServiceV3: Cache miss for product ID: " + id);
        Optional<Product> productOptional = productRepositoryV1.findById(id);

        productOptional.ifPresent(product -> {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            System.out.println("ProductServiceV3: Caching product: " + product);
        });

        System.out.println("ProductServiceV3: Fetching product from DB: " + id);
        return productOptional;
    }


    @Override
    public Product saveProduct(Product product) {
        // 1. 캐시에 먼저 반영
        String cacheKey = "product:" + product.getId(); // 새로 생성될 ID일 수도 있음 (null일 경우)
        // InMemoryProductRepository는 save 시 ID를 할당하므로, 먼저 DB에 저장하여 ID를 받아옴.
        // 실제 Write-Back에서는 DB에 쓰지 않고 캐시 매니저가 직접 ID를 관리하거나 UUID 등을 사용.
        // 여기서는 InMemoryProductRepository의 특성상 먼저 ID를 할당받는 과정을 포함.
        Product savedOrUpdatedProduct = productRepositoryV1.save(product); // 임시 저장하여 ID 확보
        cacheKey = "product:" + savedOrUpdatedProduct.getId(); // 실제 캐시 키는 할당된 ID 사용

        redisTemplate.opsForValue().set(cacheKey, savedOrUpdatedProduct, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        System.out.println("ProductServiceV3: Write-Back: Caching product immediately: " + cacheKey);

        // 2. DB에는 비동기적으로 반영
        // 실제 Write-Back은 여기서 큐에 넣거나 더티 플래그를 설정하고 즉시 반환합니다.
        // 여기서는 ExecutorService를 사용하여 비동기 DB 저장을 시뮬레이션합니다.
        writeBackExecutor.submit(() -> {
            try {
                // 실제 DB 작업이라고 가정하고 약간의 지연을 줍니다.
                Thread.sleep(500); // DB 작업 시뮬레이션 지연 (50ms)
                // productRepositoryV1.save(savedOrUpdatedProduct); // 이미 위에서 저장했으므로 다시 호출할 필요 없음.
                // 실제 DB 연동시 여기에 DB 저장 로직이 들어갑니다.
                System.out.println("ProductServiceV3: Async DB update complete for product: " + savedOrUpdatedProduct.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ProductServiceV3: Async DB update interrupted for product: " + savedOrUpdatedProduct.getId());
            }
        });

        System.out.println("ProductServiceV3: Write-Back: Immediate response after caching.");
        return savedOrUpdatedProduct;
    }

    /** 상품 수정 (Write-Back)
     * 캐시에 먼저 반영하고, DB에는 비동기적으로 반영합니다.
     */
    @Override
    public Product updateProduct(Product product) {
        if (product.getId() == null || !productRepositoryV1.findById(product.getId()).isPresent()) {
            throw new IllegalArgumentException("상품의 아이디 " + product.getId() + " - 해당 아이디의 상품을 찾지 못하였습니다.");
        }

        // 1. 캐시에 먼저 반영
        String cacheKey = "product:" + product.getId();
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        System.out.println("ProductServiceV3: Write-Back: Caching product immediately after update: " + cacheKey);

        // 2. DB에는 비동기적으로 반영
        writeBackExecutor.submit(() -> {
            try {
                Thread.sleep(500); // DB 작업 시뮬레이션 지연 (50ms)
                productRepositoryV1.save(product); // DB에 업데이트
                System.out.println("ProductServiceV3: Async DB update complete for product: " + product.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ProductServiceV3: Async DB update interrupted for product: " + product.getId());
            }
        });

        System.out.println("ProductServiceV3: Write-Back: Immediate response after caching.");
        return product; // 캐시에 저장된 product 객체를 바로 반환
    }

    /** 상품 삭제 (Write-Back)
     * 캐시에서 삭제하고, DB에도 비동기적으로 삭제를 요청합니다.
     * 주의: Write-Back의 경우 삭제도 비동기화하면 데이터 일관성이 더 복잡해집니다.
     * 여기서는 일단 동기적으로 DB를 삭제하는 것으로 구현합니다.
     * (실제 Write-Back에서 삭제는 Cache-Aside나 Write-Through와 유사하게 동기적으로 처리하는 경우가 많습니다.)
     */
    @Override
    public void deleteProduct(Long id) {
        String cacheKey = "product:" + id;

        // 1. 캐시에서 먼저 삭제
        redisTemplate.delete(cacheKey);
        System.out.println("ProductServiceV3: Deleting from cache: " + cacheKey);

        // 2. DB에는 비동기적으로 삭제 요청 (conceptually)
        writeBackExecutor.submit(() -> {
            try {
                Thread.sleep(50); // DB 작업 시뮬레이션 지연
                productRepositoryV1.deleteById(id);
                System.out.println("ProductServiceV3: Async DB delete complete for product: " + id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ProductServiceV3: Async DB delete interrupted for product: " + id);
            }
        });

        System.out.println("ProductServiceV3: Write-Back: Immediate response after deleting from cache.");
    }
}
