package mssc.beer.services.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.config.JmsConfig;
import mssc.beer.services.BeerOrderManager;
import mssc.model.event.ValidateOrderBeerResult;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderResponseListener
{
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateOrderBeerResult result)
    {
        log.debug("Result for beer order id:" + result.getId());
        beerOrderManager.processValidation(result.getId(), result.getIsValid());
    }
}
