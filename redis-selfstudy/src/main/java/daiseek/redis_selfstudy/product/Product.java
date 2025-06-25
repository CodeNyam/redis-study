package daiseek.redis_selfstudy.product;

import java.io.Serializable;

/**
 * Note. DB 없이 오직 메모리로 데이터를 저장하기 때문에 @Entity가 필요없다.
 *
 * Redis에 Product 객체를 저장할때 직렬화 과정이 필요하기에 Serializable을 상속받는다.
 */

public class Product implements Serializable {

    // 엔티티 구성속성
    private Long id;
    private String name;
    private int price;

    // 기본 생성자
    public Product() {
    }

    public Product(Long id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getter 메서드
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    // Setter 메서드 (필요시 추가, 여기서는 Immutable 객체처럼 사용)
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
