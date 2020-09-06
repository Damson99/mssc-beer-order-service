package mssc.beer.services.testcomponents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.config.JmsConfig;
import mssc.beer.repositories.BeerOrderRepository;
import mssc.beer.web.mappers.BeerOrderMapper;
import mssc.model.event.AllocateOrderRequest;
import mssc.model.event.AllocateOrderResult;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener
{
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;


    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message<AllocateOrderRequest> message)
    {
        AllocateOrderRequest allocateOrderRequest = message.getPayload();
        allocateOrderRequest.getBeerOrderDto().getBeerOrderLines()
                .forEach(beerOrderLineDto ->
                        beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity()));

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResult.builder()
                        .allocationError(false)
                        .pendingInventory(false)
                        .beerOrderDto(allocateOrderRequest.getBeerOrderDto())
                        .build());
    }
}
