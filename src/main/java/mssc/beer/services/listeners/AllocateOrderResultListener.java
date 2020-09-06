package mssc.beer.services.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.config.JmsConfig;
import mssc.beer.services.BeerOrderManager;
import mssc.model.event.AllocateOrderResult;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocateOrderResultListener
{
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResult allocateOrderResult)
    {
        if(!allocateOrderResult.getAllocationError() && !allocateOrderResult.getPendingInventory())
            beerOrderManager.beerOrderAllocationPassed(allocateOrderResult.getBeerOrderDto());
        else if(!allocateOrderResult.getAllocationError() && allocateOrderResult.getPendingInventory())
            beerOrderManager.beerOrderAllocationPendingInventory(allocateOrderResult.getBeerOrderDto());
        else if(allocateOrderResult.getAllocationError())
            beerOrderManager.beerOrderAllocationFailed(allocateOrderResult.getBeerOrderDto());
    }
}
