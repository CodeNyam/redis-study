package daiseek.redis_selfstudy.product.service;

import daiseek.redis_selfstudy.product.Product;

import java.util.Optional;

/** ProductService는 여러 구현체로 이루어진다.
 * 이 프로젝트의 목적은 레디스의 여러가지 캐시 전략을 살펴보는 것이기 때문에 코드를 보존하는게 중요하다고 생각했다.
 * 따라서 하나의 서비스 계층에 코드를 몰빵하기 보단 각 서비스 구현체마다 전략을 하나씩 공부해보는게 좋을거 같았다.
 *
 */
public interface ProductService {

    /**
     * 상품 조회 메서드
     * @param id : 조회하려는 상품의 아이디
     * @return : 직렬화된 상품 객체 데이터
     */
    public Optional<Product> getProductById(Long id);

    /**
     * 상품 생성 메서드
     * @param product : 상품 객체(직렬화된 데이터로 입력됨)
     * @return : savedProduct 객체
     */
    public Product saveProduct(Product product);

    /**
     * 상품 삭제
     * @param id : 삭제하려는 상품의 아이디
     */
    public void deleteProduct(Long id);

    /**
     * 상품 수정
     * @param product : 수정하려는 상품 객체
     * @return : 수정된 상품 객체 데이터
     */
    Product updateProduct(Product product);

}
