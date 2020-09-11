package mssc.beer.services;

import mssc.beer.domain.Customer;
import mssc.beer.repositories.CustomerRepository;
import mssc.beer.web.mappers.CustomerMapper;
import mssc.model.CustomerPagedList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService
{

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerPagedList listCustomers(Pageable pageable)
    {
        Page<Customer> customersPaged = customerRepository.findAll(pageable);
        return new CustomerPagedList(customersPaged.stream().map(customerMapper::customerToDto).collect(Collectors.toList()),
                PageRequest.of(customersPaged.getPageable().getPageNumber(), customersPaged.getPageable().getPageSize()),
                customersPaged.getTotalElements());
    }
}
