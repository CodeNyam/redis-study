package daiseek.redis_selfstudy.product.controller;

import daiseek.redis_selfstudy.product.Product;
import daiseek.redis_selfstudy.product.service.ProductService;
import daiseek.redis_selfstudy.product.service.ProductServiceV3;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class ProductController {

    private final ProductService productService; // ProductService 인터페이스 타입으로 주입

    /**
     * 생성자 주입 시 @Qualifier를 사용하여 특정 구현체 지정
     */
//    public ProductController(@Qualifier("productServiceV1") ProductService productService) {
//        this.productService = productService;
//    }

//    public ProductController(@Qualifier("productServiceV2") ProductService productService) {
//        this.productService = productService;
//    }

    public ProductController(@Qualifier("productServiceV3")ProductService productService) {
        this.productService = productService;
    }

    /**
     * 새 제품 생성 후 저장
     * POST /products
     * Request Body: { "name": "Laptop", "price": 1200000 }
     */
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = productService.saveProduct(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    /**
     * ID로 상품 조회 (Cache-Aside 전략 테스트)
     * GET /products/{id}
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * ID로 Product를 삭제 (캐시 무효화 테스트)
     * DELETE /products/{id}
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


    /**
     * 상품 수정 메서드
     * @param product : 수정하려는 상품 객체
     * @return : 수정된 상품 객체
     */
    @PutMapping("/products")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product) {
        try {
            Product updatedProduct = productService.updateProduct(product);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // 업데이트할 상품을 찾지 못한 경우
        }
    }
}
