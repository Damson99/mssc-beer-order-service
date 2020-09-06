package mssc.beer.sm.action;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mssc.beer.config.JmsConfig;
import mssc.beer.domain.BeerOrder;
import mssc.beer.domain.BeerOrderEventEnum;
import mssc.beer.domain.BeerOrderStatusEnum;
import mssc.beer.repositories.BeerOrderRepository;
import mssc.beer.services.BeerOrderManagerImpl;
import mssc.beer.web.mappers.BeerOrderMapper;
import mssc.model.event.ValidateOrderRequest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum>
{
    private final BeerOrderRepository beerOrderRepository;
    private final JmsTemplate jmsTemplate;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext)
    {
        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(UUID.fromString(beerOrderId));

        beerOrderOptional.ifPresentOrElse(beerOrder ->
        {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE,
                    ValidateOrderRequest.builder()
                    .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                    .build());
        }, () -> log.error("Order not found. Id: " + beerOrderId));

        log.debug("Sent Validation request to queue for order id:" + beerOrderId);
    }
}
