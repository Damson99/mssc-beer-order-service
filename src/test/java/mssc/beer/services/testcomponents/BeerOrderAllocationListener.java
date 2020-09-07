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
        AllocateOrderRequest request = message.getPayload();
        boolean allocationError = false;
        boolean pendingInventory = false;

        if(request.getBeerOrderDto().getCustomerRef() != null && request.getBeerOrderDto().getCustomerRef().equals("allocation-failed"))
            allocationError = true;

        if(request.getBeerOrderDto().getCustomerRef() != null && request.getBeerOrderDto().getCustomerRef().equals("partial-allocation"))
            pendingInventory = true;

        boolean finalPendingInventory = pendingInventory;
            request.getBeerOrderDto().getBeerOrderLines()
                .forEach(beerOrderLineDto ->
                        {
                            if(finalPendingInventory)
                                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
                            else
                                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
                        });

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResult.builder()
                .allocationError(allocationError)
                .pendingInventory(pendingInventory)
                .beerOrderDto(request.getBeerOrderDto())
                .build());
    }
}
