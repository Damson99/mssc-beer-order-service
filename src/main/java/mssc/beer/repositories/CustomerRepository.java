package mssc.beer.repositories;

import mssc.beer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>
{
    List<Customer> findAllByCustomerNameLike(String customerName);
}
