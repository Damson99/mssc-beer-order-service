package mssc.beer.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.domain.BeerOrder;
import mssc.beer.domain.BeerOrderLine;
import mssc.beer.domain.BeerOrderStatusEnum;
import mssc.beer.domain.Customer;
import mssc.beer.repositories.BeerOrderRepository;
import mssc.beer.repositories.CustomerRepository;
import mssc.beer.services.beer.BeerServiceImpl;
import mssc.model.BeerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@Slf4j
@SpringBootTest
@ExtendWith({WireMockExtension.class})
public class BeerOrderManagerImplIT
{
    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    JmsTemplate jmsTemplate;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();


    @BeforeEach
    void setUp()
    {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Darek")
                .build());
    }

    @TestConfiguration
    static class RestTemplateBuilderProvider
    {
        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer()
        {
            WireMockServer mockServer = with(wireMockConfig().port(8083));
            mockServer.start();
            return mockServer;
        }
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
            .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() ->
        {
            Optional<BeerOrder> foundOrderOptional = beerOrderRepository.findById(beerId);
            foundOrderOptional.ifPresentOrElse(foundOrder ->
            {
//                todo - ALLOCATED STATUS
                assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, foundOrder.getOrderStatus());
            }, () -> log.error("Order not found. Id: " + beerId));
        });

        savedBeerOrder = beerOrderRepository.findById(beerId).get();
        assertNotNull(savedBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());

    }

    public BeerOrder createBeerOrder()
    {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> beerOrderLines = new HashSet<>();
        beerOrderLines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .beerOrder(beerOrder)
                .orderQuantity(1)
                .build());

        beerOrder.setBeerOrderLines(beerOrderLines);
        return beerOrder;
    }
}
