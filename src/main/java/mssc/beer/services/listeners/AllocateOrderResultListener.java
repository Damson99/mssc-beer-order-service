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

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResult result)
    {
        if(!result.getAllocationError() && !result.getPendingInventory())
        {
            beerOrderManager.beerOrderAllocationPassed(result.getBeerOrderDto());
            log.debug("###### 1 ######");
        }
        else if(!result.getAllocationError() && result.getPendingInventory())
        {
            beerOrderManager.beerOrderAllocationPendingInventory(result.getBeerOrderDto());
            log.debug("###### 2 ######");
        }
        else if(result.getAllocationError())
        {
            beerOrderManager.beerOrderAllocationFailed(result.getBeerOrderDto());
            log.debug("###### 3 ######");
        }


    }
}
