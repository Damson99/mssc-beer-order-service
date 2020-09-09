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
        boolean sendResponse = true;

        if(request.getBeerOrderDto().getCustomerRef() != null)
        {
            switch (request.getBeerOrderDto().getCustomerRef())
            {
                case "allocation-failed":
                    allocationError = true;
                    break;
                case "partial-allocation":
                    pendingInventory = true;
                    break;
                case "dont-allocate":
                    sendResponse = false;
                    break;
            }
        }


        boolean finalPendingInventory = pendingInventory;
            request.getBeerOrderDto().getBeerOrderLines()
                .forEach(beerOrderLineDto ->
                        {
                            if(finalPendingInventory)
                                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
                            else
                                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
                        });

        if(sendResponse)
        {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                    AllocateOrderResult.builder()
                            .allocationError(allocationError)
                            .pendingInventory(pendingInventory)
                            .beerOrderDto(request.getBeerOrderDto())
                            .build());
        }
    }
}
