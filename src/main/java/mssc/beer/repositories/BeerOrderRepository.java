package mssc.beer.repositories;


import mssc.beer.domain.BeerOrder;
import mssc.beer.domain.BeerOrderStatusEnum;
import mssc.beer.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeerOrderRepository extends JpaRepository<BeerOrder, UUID>
{

    Page<BeerOrder> findAllByCustomer(Customer customer, Pageable pageable);

    List<BeerOrder> findAllByOrderStatus(BeerOrderStatusEnum orderStatusEnum);

    BeerOrder findOneById(UUID id);
}
